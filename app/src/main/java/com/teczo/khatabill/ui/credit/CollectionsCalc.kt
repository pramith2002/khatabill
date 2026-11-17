package com.teczo.khatabill.ui.credit

import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.entities.PartyEntity
import com.teczo.khatabill.data.entities.PaymentAllocationEntity
import com.teczo.khatabill.data.entities.PaymentEntity
import com.teczo.khatabill.domain.ledger.AgeBucket
import com.teczo.khatabill.domain.ledger.AgingCalculator
import com.teczo.khatabill.domain.ledger.AgingInvoice
import com.teczo.khatabill.domain.ledger.AgingRow
import com.teczo.khatabill.domain.model.BalanceType
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.domain.model.PartyType
import com.teczo.khatabill.domain.model.PaymentType

data class CollectionRow(val party: PartyEntity, val aging: AgingRow)

data class CollectionsReport(val rows: List<CollectionRow>) {
    val totals: Map<AgeBucket, Long> = AgeBucket.entries.associateWith { b -> rows.sumOf { it.aging.buckets.getValue(b) } }
    val grandTotal: Long = totals.values.sum()
}

object CollectionsCalc {
    /**
     * Ageing of what customers owe us. Only parties with something outstanding are listed, biggest first.
     * Payments received but not allocated to a bill ("on account") reduce the oldest money first.
     */
    fun compute(
        parties: List<PartyEntity>,
        invoices: List<InvoiceEntity>,
        payments: List<PaymentEntity>,
        allocations: List<PaymentAllocationEntity>,
        today: Long,
    ): CollectionsReport {
        val allocatedByPayment = allocations.groupBy { it.paymentId }.mapValues { (_, v) -> v.sumOf { it.amount } }
        val rows = parties.filter { it.type != PartyType.SUPPLIER }.mapNotNull { p ->
            val open = invoices.filter {
                it.partyId == p.id && it.type == DocumentType.SALE &&
                    (it.status == InvoiceStatus.UNPAID || it.status == InvoiceStatus.PARTIAL)
            }.map { AgingInvoice(it.grandTotal - it.paidAmount, it.date) }
            val received = payments.filter { it.partyId == p.id && it.type == PaymentType.RECEIVED }
            val unallocated = received.sumOf { it.amount - (allocatedByPayment[it.id] ?: 0L) }
            val opening = if (p.openingBalanceType == BalanceType.TO_COLLECT) p.openingBalance else 0L
            val weOweOpening = if (p.openingBalanceType == BalanceType.TO_PAY) p.openingBalance else 0L
            val aging = AgingCalculator.age(today, opening, open, unallocated + weOweOpening)
            if (aging.total > 0) CollectionRow(p, aging) else null
        }.sortedByDescending { it.aging.total }
        return CollectionsReport(rows)
    }
}
