package com.teczo.khatabill.domain.report

import com.teczo.khatabill.data.entities.ExpenseEntity
import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.entities.PaymentEntity
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.domain.model.PaymentMode
import com.teczo.khatabill.domain.model.PaymentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class DateRangesAndBooksTest {
    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()
    private fun range(kind: RangeKind, today: Long) = DateRanges.of(kind, today)
    private fun fmt(r: DateRange) = LocalDate.ofEpochDay(r.from).toString() + ".." + LocalDate.ofEpochDay(r.to)

    // ---- ranges ----

    @Test fun today() = assertEquals("2026-11-17..2026-11-17", fmt(range(RangeKind.TODAY, day(2026, 11, 17))))

    @Test fun weekRunsMondayToSunday() {
        assertEquals("2026-11-16..2026-11-22", fmt(range(RangeKind.THIS_WEEK, day(2026, 11, 17)))) // a Tuesday
        assertEquals("2026-11-16..2026-11-22", fmt(range(RangeKind.THIS_WEEK, day(2026, 11, 16)))) // Monday itself
        assertEquals("2026-11-16..2026-11-22", fmt(range(RangeKind.THIS_WEEK, day(2026, 11, 22)))) // Sunday itself
    }

    @Test fun monthIncludesTheLastDay() {
        assertEquals("2026-11-01..2026-11-30", fmt(range(RangeKind.THIS_MONTH, day(2026, 11, 17))))
        assertEquals("2028-02-01..2028-02-29", fmt(range(RangeKind.THIS_MONTH, day(2028, 2, 10)))) // leap year
    }

    @Test fun quartersFollowTheFinancialYear() {
        assertEquals("2026-10-01..2026-12-31", fmt(range(RangeKind.THIS_QUARTER, day(2026, 11, 17))))
        assertEquals("2026-04-01..2026-06-30", fmt(range(RangeKind.THIS_QUARTER, day(2026, 4, 1))))
        assertEquals("2027-01-01..2027-03-31", fmt(range(RangeKind.THIS_QUARTER, day(2027, 3, 31))))
        assertEquals("2026-07-01..2026-09-30", fmt(range(RangeKind.THIS_QUARTER, day(2026, 9, 30))))
    }

    @Test fun financialYearIsAprilToMarch() {
        assertEquals("2026-04-01..2027-03-31", fmt(range(RangeKind.THIS_FY, day(2026, 11, 17))))
        assertEquals("2025-04-01..2026-03-31", fmt(range(RangeKind.THIS_FY, day(2026, 3, 31))))
        assertEquals("2026-04-01..2027-03-31", fmt(range(RangeKind.THIS_FY, day(2026, 4, 1))))
    }

    @Test fun anotherFinancialYearStartMonthIsHonoured() {
        assertEquals("2026-01-01..2026-12-31", fmt(DateRanges.of(RangeKind.THIS_FY, day(2026, 7, 4), fyStartMonth = 1)))
    }

    @Test fun rangeContainsItsEndsAndRejectsReversedDates() {
        val r = DateRange(10, 20)
        assertEquals(true, 10L in r)
        assertEquals(true, 20L in r)
        assertEquals(false, 21L in r)
        assertThrows(IllegalArgumentException::class.java) { DateRange(5, 4) }
    }

    // ---- cash / bank book ----

    private fun pay(date: Long, amount: Long, type: PaymentType, mode: PaymentMode, id: String = "p$date$amount") =
        PaymentEntity(id = id, businessId = "b", createdAt = date, updatedAt = 0, type = type, partyId = "a", date = date, amount = amount, mode = mode)

    private fun exp(date: Long, amount: Long, mode: PaymentMode, category: String = "rent") =
        ExpenseEntity(id = "e$date$amount", businessId = "b", createdAt = date, updatedAt = 0, date = date, category = category, amount = amount, mode = mode)

    private val names = mapOf("a" to "Lakshmi")

    private fun movements(payments: List<PaymentEntity>, expenses: List<ExpenseEntity> = emptyList()) = MoneyBook.movements(payments, expenses, names)

    @Test fun cashBookRunsABalanceAndOnlyCountsCash() {
        val m = movements(
            listOf(pay(10, 1_000, PaymentType.RECEIVED, PaymentMode.CASH), pay(11, 500, PaymentType.RECEIVED, PaymentMode.UPI), pay(12, 200, PaymentType.PAID, PaymentMode.CASH)),
            listOf(exp(12, 100, PaymentMode.CASH), exp(13, 999, PaymentMode.BANK)),
        )
        val r = MoneyBook.compute(m, DateRange(10, 31), MoneyBook.CASH)
        assertEquals(0, r.opening)
        assertEquals(listOf(1_000L, 800L, 700L), r.rows.map { it.balanceAfter })
        assertEquals(1_000, r.totalIn)
        assertEquals(300, r.totalOut)
        assertEquals(700, r.closing)
    }

    @Test fun openingBalanceIsEverythingBeforeTheRange() {
        val m = movements(listOf(pay(1, 5_000, PaymentType.RECEIVED, PaymentMode.CASH), pay(20, 300, PaymentType.PAID, PaymentMode.CASH)))
        val r = MoneyBook.compute(m, DateRange(10, 30), MoneyBook.CASH)
        assertEquals(5_000, r.opening)
        assertEquals(4_700, r.closing)
        assertEquals(1, r.rows.size)
    }

    @Test fun bankBookCoversUpiCardBankAndCheque() {
        val m = movements(
            listOf(
                pay(1, 100, PaymentType.RECEIVED, PaymentMode.UPI), pay(2, 200, PaymentType.RECEIVED, PaymentMode.CARD),
                pay(3, 300, PaymentType.RECEIVED, PaymentMode.BANK), pay(4, 400, PaymentType.RECEIVED, PaymentMode.CHEQUE),
                pay(5, 999, PaymentType.RECEIVED, PaymentMode.CASH),
            ),
        )
        assertEquals(1_000, MoneyBook.compute(m, DateRange(1, 31), MoneyBook.BANK).closing)
    }

    @Test fun cashAndBankTogetherCoverEveryMode() = assertEquals(PaymentMode.entries.toSet(), MoneyBook.CASH + MoneyBook.BANK)

    @Test fun anEmptyBookIsZero() {
        val r = MoneyBook.compute(emptyList(), DateRange(1, 2), MoneyBook.CASH)
        assertEquals(0, r.closing)
        assertEquals(0, r.rows.size)
    }

    @Test fun expensesShowThePartyAndCategory() {
        val m = MoneyBook.movements(emptyList(), listOf(exp(1, 100, PaymentMode.CASH, "transport")), names)
        assertEquals("transport", m.single().label)
        assertEquals(MoneyMovement.Kind.EXPENSE, m.single().kind)
    }

    // ---- day book ----

    private fun bill(id: String, date: Long, total: Long, type: DocumentType = DocumentType.SALE, status: InvoiceStatus = InvoiceStatus.UNPAID) = InvoiceEntity(
        id = id, businessId = "b", createdAt = date * 10, updatedAt = 0, type = type, series = "INV", financialYear = "2026-27", sequenceNo = 1,
        number = id, date = date, partyName = "Lakshmi", placeOfSupplyStateCode = "29", subtotal = total, discountTotal = 0, taxableTotal = total,
        grandTotal = total, status = status,
    )

    @Test fun dayBookListsBillsPaymentsAndExpensesOfThatDayOnly() {
        val m = movements(listOf(pay(5, 700, PaymentType.RECEIVED, PaymentMode.UPI), pay(6, 1, PaymentType.RECEIVED, PaymentMode.UPI)), listOf(exp(5, 90, PaymentMode.CASH)))
        val entries = DayBook.forDay(5, listOf(bill("INV1", 5, 1_000), bill("INV2", 6, 2_000)), m)
        assertEquals(setOf(DayBookEntry.Kind.SALE, DayBookEntry.Kind.PAYMENT_IN, DayBookEntry.Kind.EXPENSE), entries.map { it.kind }.toSet())
        assertEquals(3, entries.size)
    }

    @Test fun dayBookSkipsCancelledBillsAndQuotations() {
        val entries = DayBook.forDay(
            5, listOf(bill("c", 5, 100, status = InvoiceStatus.CANCELLED), bill("q", 5, 100, type = DocumentType.QUOTATION)), emptyList(),
        )
        assertEquals(0, entries.size)
    }
}
