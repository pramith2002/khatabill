package com.teczo.khatabill.data.repositories

import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.entities.PartyEntity
import com.teczo.khatabill.data.entities.PaymentEntity
import com.teczo.khatabill.domain.ledger.StatementBill
import com.teczo.khatabill.domain.ledger.StatementBuilder
import com.teczo.khatabill.domain.ledger.StatementPayment
import com.teczo.khatabill.domain.ledger.StatementRow
import com.teczo.khatabill.domain.model.BalanceType
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.domain.model.PaymentMode
import com.teczo.khatabill.domain.model.PaymentType
import com.teczo.khatabill.ui.home.HomeSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LedgerAndStatementTest {
    private val today = LocalDate.of(2026, 11, 17).toEpochDay()

    private fun party(id: String, opening: Long = 0, type: BalanceType = BalanceType.TO_COLLECT) =
        PartyEntity(id = id, businessId = "b", createdAt = 0, updatedAt = 0, name = id, openingBalance = opening, openingBalanceType = type)

    private fun inv(
        id: String, partyId: String?, total: Long, type: DocumentType = DocumentType.SALE, paid: Long = 0,
        status: InvoiceStatus = InvoiceStatus.UNPAID, date: Long = today, due: Long? = null,
    ) = InvoiceEntity(
        id = id, businessId = "b", createdAt = 0, updatedAt = 0, type = type, series = "INV", financialYear = "2026-27",
        sequenceNo = 1, number = id, date = date, dueDate = due, partyId = partyId, placeOfSupplyStateCode = "29",
        subtotal = total, discountTotal = 0, taxableTotal = total, grandTotal = total, paidAmount = paid, status = status,
    )

    private fun pay(partyId: String?, amount: Long, type: PaymentType = PaymentType.RECEIVED, date: Long = today) =
        PaymentEntity(id = "p$amount$date", businessId = "b", createdAt = 0, updatedAt = 0, type = type, partyId = partyId, date = date, amount = amount, mode = PaymentMode.CASH)

    // ---- balances per party ----

    @Test fun balancesAreInvoicesMinusPaymentsPerParty() {
        val r = LedgerRepository.compute(
            parties = listOf(party("a", opening = 1_000), party("b")),
            invoices = listOf(inv("i1", "a", 5_000), inv("i2", "b", 2_000)),
            payments = listOf(pay("a", 1_500)),
            today = today,
        )
        assertEquals(1_000 + 5_000 - 1_500, r.byParty.getValue("a"))
        assertEquals(2_000, r.byParty.getValue("b"))
        assertEquals(4_500 + 2_000, r.toCollect)
        assertEquals(0, r.toPay)
    }

    @Test fun cancelledInvoicesDoNotCount() {
        val r = LedgerRepository.compute(
            listOf(party("a")), listOf(inv("i1", "a", 5_000, status = InvoiceStatus.CANCELLED)), emptyList(), today,
        )
        assertEquals(0, r.byParty.getValue("a"))
    }

    @Test fun suppliersWeOweShowAsToPay() {
        val r = LedgerRepository.compute(
            listOf(party("s")), listOf(inv("b1", "s", 9_000, type = DocumentType.PURCHASE)), listOf(pay("s", 3_000, PaymentType.PAID)), today,
        )
        assertEquals(-6_000, r.byParty.getValue("s"))
        assertEquals(6_000, r.toPay)
        assertEquals(0, r.toCollect)
    }

    @Test fun walkInInvoicesAndPaymentsBelongToNoParty() {
        val r = LedgerRepository.compute(listOf(party("a")), listOf(inv("i1", null, 5_000)), listOf(pay(null, 5_000)), today)
        assertEquals(0, r.byParty.getValue("a"))
    }

    @Test fun anAdvanceMakesAPartyOweNothingAndWeOweThem() {
        val r = LedgerRepository.compute(listOf(party("a")), emptyList(), listOf(pay("a", 2_000)), today)
        assertEquals(-2_000, r.byParty.getValue("a"))
        assertEquals(2_000, r.toPay)
    }

    @Test fun overdueMeansUnpaidAndPastTheDueDate() {
        val r = LedgerRepository.compute(
            listOf(party("a"), party("b")),
            listOf(
                inv("late", "a", 1_000, paid = 400, status = InvoiceStatus.PARTIAL, due = today - 3),
                inv("notYet", "b", 5_000, due = today + 3),
                inv("dueToday", "b", 7_000, due = today),
                inv("paidLate", "b", 9_000, paid = 9_000, status = InvoiceStatus.PAID, due = today - 30),
            ),
            emptyList(), today,
        )
        assertEquals(setOf("a"), r.overdueParties)
        assertEquals(600, r.overdueAmount)
    }

    // ---- home summary ----

    @Test fun homeSummaryUsesThisMonthOnly() {
        val balances = LedgerRepository.compute(listOf(party("a")), listOf(inv("i", "a", 10_000)), listOf(pay("a", 4_000)), today)
        val lastMonth = today - 40
        val s = HomeSummary.compute(
            balances,
            listOf(inv("i", "a", 10_000, date = today), inv("old", "a", 99_000, date = lastMonth), inv("x", "a", 5_000, status = InvoiceStatus.CANCELLED)),
            listOf(pay("a", 4_000, date = today), pay("a", 8_000, date = lastMonth)),
            today,
        )
        assertEquals(10_000, s.monthSales)
        assertEquals(4_000, s.monthCollected)
        assertEquals(0.4f, s.collectionRatio, 0.0001f)
    }

    @Test fun homeSummaryHasSevenDayTrendsOldestFirst() {
        val balances = LedgerRepository.compute(listOf(party("a")), emptyList(), emptyList(), today)
        val s = HomeSummary.compute(
            balances,
            listOf(inv("i1", "a", 10_000, date = today), inv("i2", "a", 2_000, date = today - 6), inv("old", "a", 9_000, date = today - 7)),
            listOf(pay("a", 4_000, date = today - 1)),
            today,
        )
        assertEquals(listOf(2_000L, 0L, 0L, 0L, 0L, 0L, 10_000L), s.salesLast7)
        assertEquals(listOf(0L, 0L, 0L, 0L, 0L, 4_000L, 0L), s.collectedLast7)
    }

    @Test fun collectionRatioIsCappedAndSafeWithNoSales() {
        assertEquals(0f, HomeSummary().collectionRatio, 0f)
        assertEquals(1f, HomeSummary(monthSales = 100, monthCollected = 500).collectionRatio, 0f)
    }

    // ---- statement ----

    @Test fun statementRunsABalanceInDateOrder() {
        val s = StatementBuilder.build(
            openingBalance = 1_000, openingType = BalanceType.TO_COLLECT,
            bills = listOf(
                StatementBill(date = 10, createdAt = 1, number = "INV/1", type = DocumentType.SALE, grandTotal = 5_000),
                StatementBill(date = 30, createdAt = 1, number = "INV/2", type = DocumentType.SALE, grandTotal = 2_000),
            ),
            payments = listOf(StatementPayment(date = 20, createdAt = 1, type = PaymentType.RECEIVED, amount = 3_000, mode = PaymentMode.UPI)),
        )
        assertEquals(1_000, s.opening)
        assertEquals(listOf(6_000L, 3_000L, 5_000L), s.rows.map { it.balanceAfter })
        assertEquals(5_000, s.closing)
        assertEquals(StatementRow.Kind.PAYMENT, s.rows[1].kind)
        assertEquals(3_000, s.rows[1].paidAmount)
        assertEquals(5_000, s.rows[0].billAmount)
    }

    @Test fun statementClosingEqualsTheBalanceCalculation() {
        val s = StatementBuilder.build(
            500, BalanceType.TO_PAY,
            listOf(StatementBill(1, 0, "P1", DocumentType.PURCHASE, 4_000), StatementBill(2, 0, "Q", DocumentType.QUOTATION, 99_999)),
            listOf(StatementPayment(3, 0, PaymentType.PAID, 1_000, PaymentMode.CASH)),
        )
        // opening -500 (we owe), purchase -4000, we paid +1000 => -3500; the quotation is ignored
        assertEquals(-500, s.opening)
        assertEquals(-3_500, s.closing)
        assertEquals(2, s.rows.size)
    }

    @Test fun sameDayEntriesKeepTheirCreationOrder() {
        val s = StatementBuilder.build(
            0, BalanceType.TO_COLLECT,
            listOf(StatementBill(5, createdAt = 2, number = "B", type = DocumentType.SALE, grandTotal = 100)),
            listOf(StatementPayment(5, createdAt = 1, type = PaymentType.RECEIVED, amount = 50, mode = PaymentMode.CASH)),
        )
        assertEquals(listOf(StatementRow.Kind.PAYMENT, StatementRow.Kind.BILL), s.rows.map { it.kind })
        assertTrue(s.closing == 50L)
    }

    @Test fun emptyStatementIsJustTheOpeningBalance() {
        val s = StatementBuilder.build(700, BalanceType.TO_COLLECT, emptyList(), emptyList())
        assertEquals(700, s.closing)
        assertEquals(0, s.rows.size)
    }
}
