package com.teczo.khatabill.domain.report

import com.teczo.khatabill.data.entities.ExpenseEntity
import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.entities.PaymentEntity
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.domain.model.PaymentMode
import com.teczo.khatabill.domain.model.PaymentType

/** One movement of money: a payment received or made, or an expense. Amounts are paise. */
data class MoneyMovement(
    val date: Long,
    val createdAt: Long,
    val kind: Kind,
    val partyName: String?,
    /** Expense category key, or the invoice number the payment belongs to when known. */
    val label: String?,
    val mode: PaymentMode,
    val amountIn: Long,
    val amountOut: Long,
) {
    enum class Kind { PAYMENT_IN, PAYMENT_OUT, EXPENSE }
}

data class BookRow(val movement: MoneyMovement, val balanceAfter: Long)

data class BookResult(val opening: Long, val rows: List<BookRow>, val closing: Long, val totalIn: Long, val totalOut: Long)

object MoneyBook {
    val CASH: Set<PaymentMode> = setOf(PaymentMode.CASH)
    val BANK: Set<PaymentMode> = setOf(PaymentMode.UPI, PaymentMode.CARD, PaymentMode.BANK, PaymentMode.CHEQUE)

    fun movements(payments: List<PaymentEntity>, expenses: List<ExpenseEntity>, partyNames: Map<String, String>): List<MoneyMovement> =
        payments.map {
            val incoming = it.type == PaymentType.RECEIVED
            MoneyMovement(
                it.date, it.createdAt, if (incoming) MoneyMovement.Kind.PAYMENT_IN else MoneyMovement.Kind.PAYMENT_OUT,
                it.partyId?.let(partyNames::get), null, it.mode, if (incoming) it.amount else 0L, if (incoming) 0L else it.amount,
            )
        } + expenses.map {
            MoneyMovement(it.date, it.createdAt, MoneyMovement.Kind.EXPENSE, it.partyId?.let(partyNames::get), it.category, it.mode, 0L, it.amount)
        }

    /**
     * A cash book or bank book for [range]: only movements paid by one of [modes]. The opening balance is
     * everything before the range, so closing balance = cash in hand (or money in the bank) at the end.
     */
    fun compute(all: List<MoneyMovement>, range: DateRange, modes: Set<PaymentMode>): BookResult {
        val relevant = all.filter { it.mode in modes }
        val opening = relevant.filter { it.date < range.from }.sumOf { it.amountIn - it.amountOut }
        var running = opening
        val rows = relevant.filter { it.date in range }
            .sortedWith(compareBy({ it.date }, { it.createdAt }))
            .map { m ->
                running += m.amountIn - m.amountOut
                BookRow(m, running)
            }
        return BookResult(opening, rows, running, rows.sumOf { it.movement.amountIn }, rows.sumOf { it.movement.amountOut })
    }
}

/** One line of the day book (everything that happened on one date). */
data class DayBookEntry(
    val kind: Kind,
    val title: String,
    val partyName: String?,
    val amount: Long,
    val mode: PaymentMode?,
    val createdAt: Long,
) {
    enum class Kind { SALE, PURCHASE, SALE_RETURN, PURCHASE_RETURN, PAYMENT_IN, PAYMENT_OUT, EXPENSE }
}

object DayBook {
    fun forDay(day: Long, invoices: List<InvoiceEntity>, movements: List<MoneyMovement>): List<DayBookEntry> {
        val bills = invoices.filter { it.date == day && it.status != InvoiceStatus.CANCELLED && it.status != InvoiceStatus.DRAFT }.mapNotNull {
            val kind = when (it.type) {
                DocumentType.SALE -> DayBookEntry.Kind.SALE
                DocumentType.PURCHASE -> DayBookEntry.Kind.PURCHASE
                DocumentType.SALE_RETURN -> DayBookEntry.Kind.SALE_RETURN
                DocumentType.PURCHASE_RETURN -> DayBookEntry.Kind.PURCHASE_RETURN
                else -> return@mapNotNull null
            }
            DayBookEntry(kind, it.number, it.partyName, it.grandTotal, null, it.createdAt)
        }
        val money = movements.filter { it.date == day }.map {
            when (it.kind) {
                MoneyMovement.Kind.PAYMENT_IN -> DayBookEntry(DayBookEntry.Kind.PAYMENT_IN, "", it.partyName, it.amountIn, it.mode, it.createdAt)
                MoneyMovement.Kind.PAYMENT_OUT -> DayBookEntry(DayBookEntry.Kind.PAYMENT_OUT, "", it.partyName, it.amountOut, it.mode, it.createdAt)
                MoneyMovement.Kind.EXPENSE -> DayBookEntry(DayBookEntry.Kind.EXPENSE, it.label.orEmpty(), it.partyName, it.amountOut, it.mode, it.createdAt)
            }
        }
        return (bills + money).sortedBy { it.createdAt }
    }
}
