package com.teczo.khatabill.ui.credit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.data.repositories.InvoiceRepository
import com.teczo.khatabill.data.repositories.PartyRepository
import com.teczo.khatabill.data.repositories.PaymentRepository
import com.teczo.khatabill.util.dates.DateFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CollectionsViewModel @Inject constructor(
    business: BusinessRepository,
    parties: PartyRepository,
    invoices: InvoiceRepository,
    payments: PaymentRepository,
) : ViewModel() {
    val report: StateFlow<CollectionsReport> = business.active.flatMapLatest { b ->
        combine(
            parties.observe(b.id), invoices.observeAll(b.id), payments.observeAll(b.id), payments.observeAllocations(b.id),
        ) { p, i, pay, alloc -> CollectionsCalc.compute(p, i, pay, alloc, DateFormat.today()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionsReport(emptyList()))
}
