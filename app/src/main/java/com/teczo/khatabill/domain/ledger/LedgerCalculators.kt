package com.teczo.khatabill.domain.ledger

import com.teczo.khatabill.domain.model.BalanceType
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.PaymentType

/**
 * Pure money logic for party balances, payment allocation and ageing. All amounts are paise.
 * Sign convention for balances: POSITIVE means the party owes us (to collect), NEGATIVE means we owe them.
 */

data class InvoiceFlow(val type: DocumentType, val grandTotal: Long)

data class PaymentFlow(val type: PaymentType, val amount: Long)

object BalanceCalculator {
    /** Balance = opening +/- invoices - payments received + payments made. Cancelled invoices must not be passed in. */
    fun balance(
        openingBalance: Long,
        openingType: BalanceType,
        invoices: List<InvoiceFlow>,
        payments: List<PaymentFlow>,
    ): Long {
        var total = if (openingType == BalanceType.TO_COLLECT) openingBalance else -openingBalance
        for (inv in invoices) {
            total += when (inv.type) {
                DocumentType.SALE, DocumentType.PURCHASE_RETURN -> inv.grandTotal
                DocumentType.PURCHASE, DocumentType.SALE_RETURN -> -inv.grandTotal
                DocumentType.QUOTATION, DocumentType.PROFORMA, DocumentType.DELIVERY_CHALLAN -> 0L
            }
        }
        for (p in payments) {
            total += if (p.type == PaymentType.RECEIVED) -p.amount else p.amount
        }
        return total
    }
}

data class OpenInvoice(val id: String, val balance: Long)

data class Allocation(val invoiceId: String, val amount: Long)

object AllocationPlanner {
    /**
     * Spreads [amount] over [openInvoices] (already sorted oldest first). If [preferredInvoiceId] is given,
     * that invoice is paid first. Whatever is left over is returned as [Plan.onAccount].
     */
    fun plan(amount: Long, openInvoices: List<OpenInvoice>, preferredInvoiceId: String? = null): Plan {
        require(amount > 0) { "Payment must be positive" }
        val ordered = if (preferredInvoiceId == null) openInvoices
        else openInvoices.sortedBy { if (it.id == preferredInvoiceId) 0 else 1 }
        var left = amount
        val out = mutableListOf<Allocation>()
        for (inv in ordered) {
            if (left == 0L) break
            if (inv.balance <= 0) continue
            val part = minOf(left, inv.balance)
            out += Allocation(inv.id, part)
            left -= part
        }
        return Plan(out, left)
    }

    data class Plan(val allocations: List<Allocation>, val onAccount: Long)
}

/** Age buckets for money still to collect. */
enum class AgeBucket { DAYS_0_30, DAYS_31_60, DAYS_61_90, DAYS_90_PLUS }

data class AgingInvoice(val balance: Long, /** epoch day of the invoice */ val date: Long)

data class AgingRow(val buckets: Map<AgeBucket, Long>) {
    val total: Long get() = buckets.values.sum()
}

object AgingCalculator {
    fun bucketFor(ageDays: Long): AgeBucket = when {
        ageDays <= 30 -> AgeBucket.DAYS_0_30
        ageDays <= 60 -> AgeBucket.DAYS_31_60
        ageDays <= 90 -> AgeBucket.DAYS_61_90
        else -> AgeBucket.DAYS_90_PLUS
    }

    /**
     * Ageing of what one party owes us. The opening balance counts as the oldest money, and any
     * unallocated credit (payments on account) is taken off the oldest buckets first.
     */
    fun age(today: Long, openingToCollect: Long, openInvoices: List<AgingInvoice>, creditOnAccount: Long): AgingRow {
        val buckets = AgeBucket.entries.associateWith { 0L }.toMutableMap()
        buckets[AgeBucket.DAYS_90_PLUS] = openingToCollect.coerceAtLeast(0)
        for (inv in openInvoices) {
            if (inv.balance <= 0) continue
            val bucket = bucketFor((today - inv.date).coerceAtLeast(0))
            buckets[bucket] = buckets.getValue(bucket) + inv.balance
        }
        var credit = creditOnAccount.coerceAtLeast(0)
        for (bucket in listOf(AgeBucket.DAYS_90_PLUS, AgeBucket.DAYS_61_90, AgeBucket.DAYS_31_60, AgeBucket.DAYS_0_30)) {
            if (credit == 0L) break
            val take = minOf(credit, buckets.getValue(bucket))
            buckets[bucket] = buckets.getValue(bucket) - take
            credit -= take
        }
        return AgingRow(buckets)
    }
}
