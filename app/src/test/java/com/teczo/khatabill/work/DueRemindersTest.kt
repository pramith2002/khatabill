package com.teczo.khatabill.work

import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.entities.PartyEntity
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.InvoiceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class DueRemindersTest {
    private val today = 1_000L

    private fun party(id: String, name: String = id, kn: String? = null) =
        PartyEntity(id = id, businessId = "b", createdAt = 0, updatedAt = 0, name = name, nameKn = kn)

    private fun inv(id: String, partyId: String?, total: Long, due: Long?, paid: Long = 0) = InvoiceEntity(
        id = id, businessId = "b", createdAt = 0, updatedAt = 0, type = DocumentType.SALE, series = "INV", financialYear = "2026-27",
        sequenceNo = 1, number = id, date = 0, dueDate = due, partyId = partyId, placeOfSupplyStateCode = "29", subtotal = total,
        discountTotal = 0, taxableTotal = total, grandTotal = total, paidAmount = paid,
        status = if (paid > 0) InvoiceStatus.PARTIAL else InvoiceStatus.UNPAID,
    )

    private fun plan(vararg invoices: InvoiceEntity, parties: List<PartyEntity> = listOf(party("a"), party("b")), already: Set<String> = emptySet()) =
        DueReminders.plan(invoices.toList(), parties.associateBy { it.id }, already, today)

    @Test fun dueTodayIsNotOverdue() {
        val n = plan(inv("i1", "a", 12_400_00, today)).single()
        assertEquals(12_400_00, n.amount)
        assertTrue(!n.overdue)
    }

    @Test fun pastDueIsOverdue() = assertTrue(plan(inv("i1", "a", 500, today - 2)).single().overdue)

    @Test fun futureAndUndatedBillsAreIgnored() = assertEquals(0, plan(inv("f", "a", 500, today + 1), inv("n", "a", 500, null)).size)

    @Test fun fullyPaidBillsAreIgnored() = assertEquals(0, plan(inv("p", "a", 500, today - 1, paid = 500)).size)

    @Test fun oneNoticePerPartyWithTheUnpaidBalanceAdded() {
        val n = plan(inv("i1", "a", 1_000, today, paid = 400), inv("i2", "a", 2_000, today - 5)).single()
        assertEquals(2_600, n.amount)
        assertTrue(n.overdue)
        assertEquals(listOf("i1", "i2"), n.invoiceIds)
    }

    @Test fun billsAlreadyNotifiedTodayAreSkipped() {
        val r = plan(inv("i1", "a", 1_000, today), inv("i2", "b", 700, today), already = setOf("i1"))
        assertEquals(listOf("b"), r.map { it.partyId })
    }

    @Test fun walkInBillsAndUnknownPartiesAreSkipped() {
        assertEquals(0, plan(inv("w", null, 500, today), inv("x", "ghost", 500, today)).size)
    }

    @Test fun biggestAmountFirst() {
        val r = plan(inv("i1", "a", 100, today), inv("i2", "b", 9_000, today))
        assertEquals(listOf("b", "a"), r.map { it.partyId })
    }

    @Test fun kannadaNameIsCarriedForTheNotification() {
        val n = plan(inv("i1", "a", 100, today), parties = listOf(party("a", "Ramesh", "ರಮೇಶ್"))).single()
        assertEquals("ರಮೇಶ್", n.partyNameKn)
    }

    // ---- schedule ----

    @Test fun nextNineAmIsTodayWhenItHasNotPassed() {
        val d = ReminderScheduler.delayToNextNineAm(LocalDateTime.of(2026, 11, 17, 7, 30))
        assertEquals(90, d.toMinutes())
    }

    @Test fun nextNineAmIsTomorrowWhenItHasPassed() {
        val d = ReminderScheduler.delayToNextNineAm(LocalDateTime.of(2026, 11, 17, 9, 0))
        assertEquals(24 * 60, d.toMinutes())
        val late = ReminderScheduler.delayToNextNineAm(LocalDateTime.of(2026, 11, 17, 21, 0))
        assertEquals(12 * 60, late.toMinutes())
    }
}
