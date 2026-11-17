package com.teczo.khatabill.ui.parties

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.entities.PartyEntity
import com.teczo.khatabill.data.entities.PaymentEntity
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.data.repositories.InvoiceRepository
import com.teczo.khatabill.data.repositories.PartyRepository
import com.teczo.khatabill.data.repositories.PaymentRepository
import com.teczo.khatabill.domain.ledger.BalanceCalculator
import com.teczo.khatabill.domain.ledger.InvoiceFlow
import com.teczo.khatabill.domain.ledger.PaymentFlow
import com.teczo.khatabill.domain.ledger.Statement
import com.teczo.khatabill.domain.ledger.StatementBill
import com.teczo.khatabill.domain.ledger.StatementBuilder
import com.teczo.khatabill.domain.ledger.StatementPayment
import com.teczo.khatabill.domain.model.InvoiceStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PartyDetailState(
    val party: PartyEntity? = null,
    val invoices: List<InvoiceEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    /** Positive = they owe us. */
    val balance: Long = 0,
    val statement: Statement? = null,
)

@HiltViewModel
class PartyDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    parties: PartyRepository,
    invoices: InvoiceRepository,
    payments: PaymentRepository,
    businessRepository: BusinessRepository,
) : ViewModel() {
    val partyId: String = checkNotNull(savedState["partyId"])

    val state: StateFlow<PartyDetailState> =
        combine(parties.observeById(partyId), invoices.observeByParty(partyId), payments.observeByParty(partyId)) { party, inv, pay ->
            if (party == null) return@combine PartyDetailState()
            val live = inv.filter { it.status != InvoiceStatus.CANCELLED && it.status != InvoiceStatus.DRAFT }
            val balance = BalanceCalculator.balance(
                party.openingBalance, party.openingBalanceType,
                live.map { InvoiceFlow(it.type, it.grandTotal) }, pay.map { PaymentFlow(it.type, it.amount) },
            )
            val statement = StatementBuilder.build(
                party.openingBalance, party.openingBalanceType,
                live.map { StatementBill(it.date, it.createdAt, it.number, it.type, it.grandTotal) },
                pay.map { StatementPayment(it.date, it.createdAt, it.type, it.amount, it.mode) },
            )
            PartyDetailState(party, inv, pay, balance, statement)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PartyDetailState())

    val business: StateFlow<BusinessEntity?> =
        businessRepository.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
