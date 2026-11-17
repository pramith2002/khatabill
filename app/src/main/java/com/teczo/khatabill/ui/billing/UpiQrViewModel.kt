package com.teczo.khatabill.ui.billing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.data.dao.InvoiceWithLines
import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.data.repositories.InvoiceRepository
import com.teczo.khatabill.data.repositories.PaymentRepository
import com.teczo.khatabill.domain.model.PaymentMode
import com.teczo.khatabill.util.dates.DateFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpiQrViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val businessRepository: BusinessRepository,
    invoices: InvoiceRepository,
    private val payments: PaymentRepository,
) : ViewModel() {
    private val invoiceId: String = checkNotNull(savedState["invoiceId"])

    val data: StateFlow<InvoiceWithLines?> =
        invoices.observeWithLines(invoiceId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val business: StateFlow<BusinessEntity?> =
        businessRepository.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _paid = MutableStateFlow(false)
    val paid: StateFlow<Boolean> = _paid.asStateFlow()

    /** "Mark as paid": UPI does not report back reliably, so the shopkeeper confirms by hand. */
    fun markPaid(amount: Long) {
        viewModelScope.launch {
            payments.recordAgainstInvoice(invoiceId, amount, PaymentMode.UPI, DateFormat.today())
            _paid.value = true
        }
    }

    fun saveUpiId(upiId: String) {
        viewModelScope.launch {
            val biz = businessRepository.active.first()
            businessRepository.save(biz.copy(upiId = upiId.trim()))
        }
    }
}
