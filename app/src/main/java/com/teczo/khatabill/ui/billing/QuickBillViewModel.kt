package com.teczo.khatabill.ui.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.R
import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.data.repositories.InvoiceDraft
import com.teczo.khatabill.data.repositories.InvoiceLineDraft
import com.teczo.khatabill.data.repositories.InvoiceRepository
import com.teczo.khatabill.domain.invoice.InvoiceNumber
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.GstScheme
import com.teczo.khatabill.util.dates.DateFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Fast walk-in sale: amount, GST rate, cash or UPI, one tap. The bill is saved as paid in full. */
@HiltViewModel
class QuickBillViewModel @Inject constructor(
    private val businessRepository: BusinessRepository,
    private val invoiceRepository: InvoiceRepository,
) : ViewModel() {
    private val _business = MutableStateFlow<BusinessEntity?>(null)
    val business: StateFlow<BusinessEntity?> = _business.asStateFlow()

    private val _form = MutableStateFlow(QuickBillForm())
    val form: StateFlow<QuickBillForm> = _form.asStateFlow()

    private val _error = MutableStateFlow<Int?>(null)
    val error: StateFlow<Int?> = _error.asStateFlow()

    private val _savedId = MutableStateFlow<String?>(null)
    val savedId: StateFlow<String?> = _savedId.asStateFlow()

    private val _lineName = MutableStateFlow("Quick sale")
    private var saving = false

    /** The calculation behind the total shown on screen. */
    private val _calc = MutableStateFlow<CalcState>(CalcState.Empty)
    val calc: StateFlow<CalcState> = _calc.asStateFlow()

    init {
        viewModelScope.launch {
            _business.value = businessRepository.active.first()
            combine(_form, _business, _lineName) { f, b, n -> Triple(f, b, n) }.collect { (f, b, n) ->
                _calc.value = if (b == null || f.amount.isBlank()) CalcState.Empty
                else buildQuickCalc(f, b, DateFormat.today(), n)
            }
        }
    }

    /** The localized name given to the bill line when the user types no description. */
    fun setLineName(name: String) { _lineName.value = name }

    fun update(change: (QuickBillForm) -> QuickBillForm) {
        _form.update(change)
        _error.value = null
    }

    fun save() {
        val ok = _calc.value as? CalcState.Ok ?: return
        val biz = _business.value ?: return
        if (saving) return
        saving = true
        val f = _form.value
        val r = ok.result
        val charges = biz.gstScheme == GstScheme.REGULAR
        val res = r.lines.first()
        val draft = InvoiceDraft(
            businessId = biz.id,
            type = DocumentType.SALE,
            series = InvoiceNumber.defaultSeries(DocumentType.SALE, charges),
            prefix = biz.invoicePrefix,
            financialYearStartMonth = biz.financialYearStartMonth,
            date = DateFormat.today(),
            dueDate = null,
            partyId = null,
            partyName = null,
            partyGstin = null,
            partyAddress = null,
            partyStateCode = null,
            placeOfSupplyStateCode = biz.stateCode,
            subtotal = r.subtotal,
            discountTotal = r.discountTotal,
            otherCharges = r.otherCharges,
            taxableTotal = r.taxableTotal,
            cgst = r.cgst,
            sgst = r.sgst,
            igst = r.igst,
            roundOff = r.roundOff,
            grandTotal = r.grandTotal,
            // Quick bills are paid at the counter, in full.
            paidAmount = r.grandTotal,
            paymentMode = f.paymentMode,
            notes = null,
            terms = null,
            transportDetails = null,
            vehicleNo = null,
            lines = listOf(
                InvoiceLineDraft(
                    itemId = null,
                    nameSnapshot = f.description.trim().ifBlank { _lineName.value },
                    hsnSac = null,
                    qty = ok.qtys.first(),
                    unit = "nos",
                    rate = ok.rates.first(),
                    taxInclusive = charges,
                    discountPercentBp = 0,
                    discountAmount = res.discountAmount,
                    taxableValue = res.taxableValue,
                    gstRateBp = if (charges) f.gstRateBp else 0,
                    cgst = res.cgst,
                    sgst = res.sgst,
                    igst = res.igst,
                    lineTotal = res.lineTotal,
                    affectsStock = false,
                ),
            ),
        )
        viewModelScope.launch {
            try {
                _savedId.value = invoiceRepository.createInvoice(draft).id
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _error.value = R.string.err_save_failed
            } finally {
                saving = false
            }
        }
    }
}
