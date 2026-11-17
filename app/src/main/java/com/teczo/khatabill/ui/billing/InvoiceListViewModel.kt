package com.teczo.khatabill.ui.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.data.repositories.InvoiceRepository
import com.teczo.khatabill.domain.model.DocumentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** The document types that can be created in Phase 1. */
val billingTabs = listOf(DocumentType.SALE, DocumentType.PURCHASE, DocumentType.QUOTATION)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InvoiceListViewModel @Inject constructor(
    business: BusinessRepository,
    invoices: InvoiceRepository,
) : ViewModel() {
    val type = MutableStateFlow(DocumentType.SALE)

    val items: StateFlow<List<InvoiceEntity>> = combine(business.active, type) { b, t -> b.id to t }
        .flatMapLatest { (businessId, t) -> invoices.observeByType(businessId, t) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
