package com.teczo.khatabill.ui.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.data.entities.PaymentEntity
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.data.repositories.PartyRepository
import com.teczo.khatabill.data.repositories.PaymentRepository
import com.teczo.khatabill.domain.model.PaymentMode
import com.teczo.khatabill.domain.model.PaymentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentRow(val payment: PaymentEntity, val partyName: String?)

data class PaymentListState(
    val rows: List<PaymentRow> = emptyList(),
    /** Money received, grouped by how it was paid (cash, UPI, ...). */
    val receivedByMode: Map<PaymentMode, Long> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PaymentListViewModel @Inject constructor(
    business: BusinessRepository,
    parties: PartyRepository,
    private val payments: PaymentRepository,
) : ViewModel() {
    val state: StateFlow<PaymentListState> = business.active.flatMapLatest { b ->
        combine(payments.observeAll(b.id), parties.observe(b.id)) { list, ps ->
            val names = ps.associate { it.id to it.name }
            PaymentListState(
                rows = list.map { PaymentRow(it, it.partyId?.let(names::get)) },
                receivedByMode = list.filter { it.type == PaymentType.RECEIVED }
                    .groupBy { it.mode }.mapValues { (_, v) -> v.sumOf { it.amount } },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PaymentListState())

    fun delete(paymentId: String) {
        viewModelScope.launch { payments.delete(paymentId) }
    }
}
