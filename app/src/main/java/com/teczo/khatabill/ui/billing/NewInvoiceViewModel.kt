package com.teczo.khatabill.ui.billing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.R
import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.data.entities.ItemEntity
import com.teczo.khatabill.data.entities.PartyEntity
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.data.repositories.InvoiceDraft
import com.teczo.khatabill.data.repositories.InvoiceLineDraft
import com.teczo.khatabill.data.repositories.InvoiceRepository
import com.teczo.khatabill.data.repositories.ItemRepository
import com.teczo.khatabill.data.repositories.LedgerRepository
import com.teczo.khatabill.data.repositories.PartyRepository
import com.teczo.khatabill.domain.invoice.InvoiceNumber
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.GstScheme
import com.teczo.khatabill.domain.model.ItemType
import com.teczo.khatabill.domain.model.PartyType
import com.teczo.khatabill.util.dates.DateFormat
import com.teczo.khatabill.util.money.MoneyInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** The party's credit limit and what their balance would be after this bill (both paise). */
data class CreditWarning(val limit: Long, val balanceAfter: Long)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NewInvoiceViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val businessRepository: BusinessRepository,
    partyRepository: PartyRepository,
    itemRepository: ItemRepository,
    private val invoiceRepository: InvoiceRepository,
    ledger: LedgerRepository,
) : ViewModel() {
    private val type: DocumentType = DocumentType.valueOf(savedState["type"] ?: DocumentType.SALE.name)

    private val _business = MutableStateFlow<BusinessEntity?>(null)
    val business: StateFlow<BusinessEntity?> = _business.asStateFlow()

    private val _form = MutableStateFlow(InvoiceFormState(type = type, date = DateFormat.today()))
    val form: StateFlow<InvoiceFormState> = _form.asStateFlow()

    val calc: StateFlow<CalcState> = combine(_form, _business) { form, biz ->
        if (biz == null) CalcState.Empty else buildCalc(form, biz)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CalcState.Empty)

    /** Parties offered in the picker: customers for sales and quotations, suppliers for purchases. */
    val parties: StateFlow<List<PartyEntity>> = businessRepository.active
        .flatMapLatest { partyRepository.observe(it.id) }
        .map { list ->
            list.filter {
                if (type == DocumentType.PURCHASE) it.type != PartyType.CUSTOMER else it.type != PartyType.SUPPLIER
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val items: StateFlow<List<ItemEntity>> = businessRepository.active
        .flatMapLatest { itemRepository.observe(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Shown when this bill would take the party past their credit limit (warn only; the bill can still be saved). */
    val creditWarning: StateFlow<CreditWarning?> = businessRepository.active.flatMapLatest { b ->
        combine(_form, calc, ledger.observeBalances(b.id)) { f, c, balances ->
            val party = f.party
            val limit = party?.creditLimit
            if (type != DocumentType.SALE || party == null || limit == null || c !is CalcState.Ok) {
                null
            } else {
                val paid = MoneyInput.parsePaise(f.paid) ?: 0L
                val after = (balances.byParty[party.id] ?: 0L) + c.result.grandTotal - paid
                if (after > limit) CreditWarning(limit, after) else null
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _error = MutableStateFlow<Int?>(null)
    val error: StateFlow<Int?> = _error.asStateFlow()

    private val _savedId = MutableStateFlow<String?>(null)
    val savedId: StateFlow<String?> = _savedId.asStateFlow()

    init {
        viewModelScope.launch {
            val biz = businessRepository.active.first()
            _business.value = biz
            _form.update { it.copy(placeOfSupply = biz.stateCode) }
        }
    }

    fun update(change: (InvoiceFormState) -> InvoiceFormState) {
        _form.update(change)
        _error.value = null
    }

    fun setParty(party: PartyEntity?) {
        val biz = _business.value
        update { it.copy(party = party, placeOfSupply = party?.stateCode ?: biz?.stateCode.orEmpty()) }
    }

    fun updateLine(key: String, change: (LineState) -> LineState) =
        update { f -> f.copy(lines = f.lines.map { if (it.key == key) change(it) else it }) }

    fun removeLine(key: String) = update { f -> f.copy(lines = f.lines.filterNot { it.key == key }) }

    /** Tapping the same item again adds one more unit instead of a second line. */
    fun addItem(item: ItemEntity) {
        val biz = _business.value ?: return
        val charges = biz.gstScheme == GstScheme.REGULAR
        update { f ->
            val existing = f.lines.firstOrNull { it.itemId == item.id }
            if (existing != null) {
                val qty = MoneyInput.parseQty(existing.qty) ?: 0L
                f.copy(lines = f.lines.map { if (it.key == existing.key) it.copy(qty = MoneyInput.qtyToText(qty + 1_000)) else it })
            } else {
                val price = if (type == DocumentType.PURCHASE && item.purchasePrice > 0) item.purchasePrice else item.salePrice
                f.copy(
                    lines = f.lines + LineState(
                        key = UUID.randomUUID().toString(),
                        itemId = item.id,
                        name = item.name,
                        hsn = item.hsnSac,
                        unit = item.unit,
                        gstRateBp = if (charges) item.gstRateBp else 0,
                        taxInclusive = charges && item.taxInclusive,
                        affectsStock = item.type == ItemType.PRODUCT,
                        qty = "1",
                        rate = MoneyInput.paiseToText(price),
                    ),
                )
            }
        }
    }

    fun save() {
        val ok = calc.value as? CalcState.Ok ?: return
        val biz = _business.value ?: return
        val f = _form.value
        val r = ok.result

        val paid = when {
            type != DocumentType.SALE || f.paid.isBlank() -> 0L
            else -> MoneyInput.parsePaise(f.paid) ?: run { _error.value = R.string.err_number; return }
        }
        if (paid > r.grandTotal) {
            _error.value = R.string.err_paid_more
            return
        }

        val charges = biz.gstScheme == GstScheme.REGULAR
        val draft = InvoiceDraft(
            businessId = biz.id,
            type = type,
            series = InvoiceNumber.defaultSeries(type, charges),
            prefix = biz.invoicePrefix,
            financialYearStartMonth = biz.financialYearStartMonth,
            date = f.date,
            dueDate = f.dueDate.takeIf { type == DocumentType.SALE },
            partyId = f.party?.id,
            partyName = f.party?.name,
            partyGstin = f.party?.gstin,
            partyAddress = f.party?.billingAddress,
            partyStateCode = f.party?.stateCode,
            placeOfSupplyStateCode = f.placeOfSupply,
            subtotal = r.subtotal,
            discountTotal = r.discountTotal,
            otherCharges = r.otherCharges,
            taxableTotal = r.taxableTotal,
            cgst = r.cgst,
            sgst = r.sgst,
            igst = r.igst,
            roundOff = r.roundOff,
            grandTotal = r.grandTotal,
            paidAmount = paid,
            paymentMode = f.paymentMode,
            notes = f.notes.trim().ifBlank { null },
            terms = null,
            transportDetails = null,
            vehicleNo = null,
            lines = f.lines.mapIndexed { i, line ->
                val res = r.lines[i]
                InvoiceLineDraft(
                    itemId = line.itemId,
                    nameSnapshot = line.name,
                    hsnSac = line.hsn,
                    qty = ok.qtys[i],
                    unit = line.unit,
                    rate = ok.rates[i],
                    taxInclusive = line.taxInclusive && charges,
                    discountPercentBp = ok.discountPercentBp[i],
                    discountAmount = res.discountAmount,
                    taxableValue = res.taxableValue,
                    gstRateBp = if (charges) line.gstRateBp else 0,
                    cgst = res.cgst,
                    sgst = res.sgst,
                    igst = res.igst,
                    lineTotal = res.lineTotal,
                    affectsStock = line.affectsStock,
                )
            },
        )
        viewModelScope.launch { _savedId.value = invoiceRepository.createInvoice(draft).id }
    }
}
