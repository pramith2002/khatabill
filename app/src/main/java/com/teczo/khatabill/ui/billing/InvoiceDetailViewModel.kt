package com.teczo.khatabill.ui.billing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.data.dao.InvoiceWithLines
import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.data.repositories.InvoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvoiceDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    businessRepository: BusinessRepository,
    private val invoices: InvoiceRepository,
    private val parties: com.teczo.khatabill.data.repositories.PartyRepository,
) : ViewModel() {
    private val invoiceId: String = checkNotNull(savedState["invoiceId"])

    val data: StateFlow<InvoiceWithLines?> =
        invoices.observeWithLines(invoiceId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The customer on this bill (for their WhatsApp number), if the bill has one. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val party: StateFlow<com.teczo.khatabill.data.entities.PartyEntity?> = data
        .flatMapLatest { d -> d?.invoice?.partyId?.let { parties.observeById(it) } ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val business: StateFlow<BusinessEntity?> =
        businessRepository.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun cancel(reason: String) {
        viewModelScope.launch { invoices.cancelInvoice(invoiceId, reason) }
    }
}
