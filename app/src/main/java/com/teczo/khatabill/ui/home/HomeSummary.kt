package com.teczo.khatabill.ui.home

import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.entities.PaymentEntity
import com.teczo.khatabill.data.repositories.PartyBalances
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.domain.model.PaymentType
import java.time.LocalDate

/** The numbers shown on Home. All paise. */
data class HomeSummary(
    val toCollect: Long = 0,
    val toPay: Long = 0,
    val overdue: Long = 0,
    val monthSales: Long = 0,
    val monthCollected: Long = 0,
    val todaySales: Long = 0,
    val todayCollected: Long = 0,
) {
    /** Share of this month's sales that has been collected, 0..1 (the progress ring). */
    val collectionRatio: Float
        get() = if (monthSales <= 0) 0f else (monthCollected.toFloat() / monthSales).coerceIn(0f, 1f)

    companion object {
        fun compute(balances: PartyBalances, invoices: List<InvoiceEntity>, payments: List<PaymentEntity>, today: Long): HomeSummary {
            val d = LocalDate.ofEpochDay(today)
            val monthStart = d.withDayOfMonth(1).toEpochDay()
            val monthEnd = d.withDayOfMonth(d.lengthOfMonth()).toEpochDay()
            val sales = invoices.filter { it.type == DocumentType.SALE && it.status != InvoiceStatus.CANCELLED && it.status != InvoiceStatus.DRAFT }
            val received = payments.filter { it.type == PaymentType.RECEIVED }
            return HomeSummary(
                toCollect = balances.toCollect,
                toPay = balances.toPay,
                overdue = balances.overdueAmount,
                monthSales = sales.filter { it.date in monthStart..monthEnd }.sumOf { it.grandTotal },
                monthCollected = received.filter { it.date in monthStart..monthEnd }.sumOf { it.amount },
                todaySales = sales.filter { it.date == today }.sumOf { it.grandTotal },
                todayCollected = received.filter { it.date == today }.sumOf { it.amount },
            )
        }
    }
}
