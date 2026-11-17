package com.teczo.khatabill.domain.ledger

import com.teczo.khatabill.domain.model.BalanceType
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.PaymentMode
import com.teczo.khatabill.domain.model.PaymentType

/** One line of a party statement: a bill or a payment, with the running balance after it. */
data class StatementRow(
    /** Epoch day. */
    val date: Long,
    val kind: Kind,
    /** Invoice number for bills, empty for payments. */
    val reference: String,
    val mode: PaymentMode? = null,
    /** Increase of what the party owes us (a bill). */
    val billAmount: Long,
    /** Decrease of what the party owes us (a payment received or a return). */
    val paidAmount: Long,
    val balanceAfter: Long,
) {
    enum class Kind { BILL, PAYMENT }
}

data class Statement(val opening: Long, val rows: List<StatementRow>, val closing: Long)

data class StatementBill(val date: Long, val createdAt: Long, val number: String, val type: DocumentType, val grandTotal: Long)

data class StatementPayment(val date: Long, val createdAt: Long, val type: PaymentType, val amount: Long, val mode: PaymentMode)

object StatementBuilder {
    /**
     * Builds a statement for one party in date order. Balance convention matches [BalanceCalculator]:
     * positive = the party owes us. Bills that do not move money (quotations etc.) are left out.
     */
    fun build(
        openingBalance: Long,
        openingType: BalanceType,
        bills: List<StatementBill>,
        payments: List<StatementPayment>,
    ): Statement {
        data class Event(val date: Long, val created: Long, val row: (Long) -> StatementRow, val delta: Long)

        val events = mutableListOf<Event>()
        for (b in bills) {
            val delta = when (b.type) {
                DocumentType.SALE, DocumentType.PURCHASE_RETURN -> b.grandTotal
                DocumentType.PURCHASE, DocumentType.SALE_RETURN -> -b.grandTotal
                else -> continue
            }
            events += Event(b.date, b.createdAt, { after ->
                StatementRow(b.date, StatementRow.Kind.BILL, b.number, null, billAmount = delta.coerceAtLeast(0), paidAmount = (-delta).coerceAtLeast(0), balanceAfter = after)
            }, delta)
        }
        for (p in payments) {
            val delta = if (p.type == PaymentType.RECEIVED) -p.amount else p.amount
            events += Event(p.date, p.createdAt, { after ->
                StatementRow(p.date, StatementRow.Kind.PAYMENT, "", p.mode, billAmount = delta.coerceAtLeast(0), paidAmount = (-delta).coerceAtLeast(0), balanceAfter = after)
            }, delta)
        }
        val opening = if (openingType == BalanceType.TO_COLLECT) openingBalance else -openingBalance
        var running = opening
        val rows = events.sortedWith(compareBy({ it.date }, { it.created })).map { e ->
            running += e.delta
            e.row(running)
        }
        return Statement(opening, rows, running)
    }
}
