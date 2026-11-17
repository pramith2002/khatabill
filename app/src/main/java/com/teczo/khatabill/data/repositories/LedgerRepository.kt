package com.teczo.khatabill.data.repositories

import com.teczo.khatabill.data.dao.InvoiceDao
import com.teczo.khatabill.data.dao.PartyDao
import com.teczo.khatabill.data.dao.PaymentDao
import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.entities.PartyEntity
import com.teczo.khatabill.data.entities.PaymentEntity
import com.teczo.khatabill.domain.ledger.BalanceCalculator
import com.teczo.khatabill.domain.ledger.InvoiceFlow
import com.teczo.khatabill.domain.ledger.PaymentFlow
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.util.dates.DateFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/** Balances of every party. Positive = they owe us (to collect), negative = we owe them. Never stored. */
data class PartyBalances(
    val byParty: Map<String, Long>,
    /** Parties that have an unpaid sale past its due date. */
    val overdueParties: Set<String>,
    /** Sum of all unpaid balances of sales past their due date. */
    val overdueAmount: Long,
) {
    val toCollect: Long get() = byParty.values.filter { it > 0 }.sum()
    val toPay: Long get() = byParty.values.filter { it < 0 }.sumOf { -it }
}

@Singleton
class LedgerRepository @Inject constructor(
    private val partyDao: PartyDao,
    private val invoiceDao: InvoiceDao,
    private val paymentDao: PaymentDao,
) {
    fun observeBalances(businessId: String): Flow<PartyBalances> =
        combine(partyDao.observe(businessId, ""), invoiceDao.observeAll(businessId), paymentDao.observeAll(businessId)) { parties, invoices, payments ->
            compute(parties, invoices, payments, DateFormat.today())
        }

    companion object {
        /** Pure, so the grouping can be unit tested without a database. */
        fun compute(
            parties: List<PartyEntity>,
            invoices: List<InvoiceEntity>,
            payments: List<PaymentEntity>,
            today: Long,
        ): PartyBalances {
            val live = invoices.filter { it.status != InvoiceStatus.CANCELLED && it.status != InvoiceStatus.DRAFT }
            val invByParty = live.filter { it.partyId != null }.groupBy { it.partyId!! }
            val payByParty = payments.filter { it.partyId != null }.groupBy { it.partyId!! }
            val balances = parties.associate { p ->
                p.id to BalanceCalculator.balance(
                    openingBalance = p.openingBalance,
                    openingType = p.openingBalanceType,
                    invoices = invByParty[p.id].orEmpty().map { InvoiceFlow(it.type, it.grandTotal) },
                    payments = payByParty[p.id].orEmpty().map { PaymentFlow(it.type, it.amount) },
                )
            }
            val overdue = live.filter {
                it.type == DocumentType.SALE && it.dueDate != null && it.dueDate < today && it.grandTotal > it.paidAmount
            }
            return PartyBalances(
                byParty = balances,
                overdueParties = overdue.mapNotNull { it.partyId }.toSet(),
                overdueAmount = overdue.sumOf { it.grandTotal - it.paidAmount },
            )
        }
    }
}
