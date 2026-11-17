package com.teczo.khatabill.ui.credit

import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.entities.PartyEntity
import com.teczo.khatabill.data.entities.PaymentAllocationEntity
import com.teczo.khatabill.data.entities.PaymentEntity
import com.teczo.khatabill.domain.ledger.AgeBucket
import com.teczo.khatabill.domain.model.BalanceType
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.domain.model.PartyType
import com.teczo.khatabill.domain.model.PaymentMode
import com.teczo.khatabill.domain.model.PaymentType
import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionsCalcTest {
    private val today = 1_000L

    private fun party(id: String, opening: Long = 0, type: BalanceType = BalanceType.TO_COLLECT, kind: PartyType = PartyType.CUSTOMER) =
        PartyEntity(id = id, businessId = "b", createdAt = 0, updatedAt = 0, name = id, type = kind, openingBalance = opening, openingBalanceType = type)

    private fun sale(id: String, partyId: String, total: Long, ageDays: Long, paid: Long = 0, status: InvoiceStatus = InvoiceStatus.UNPAID) =
        InvoiceEntity(
            id = id, businessId = "b", createdAt = 0, updatedAt = 0, type = DocumentType.SALE, series = "INV", financialYear = "2026-27",
            sequenceNo = 1, number = id, date = today - ageDays, partyId = partyId, placeOfSupplyStateCode = "29", subtotal = total,
            discountTotal = 0, taxableTotal = total, grandTotal = total, paidAmount = paid, status = status,
        )

    private fun pay(id: String, partyId: String, amount: Long) = PaymentEntity(
        id = id, businessId = "b", createdAt = 0, updatedAt = 0, type = PaymentType.RECEIVED, partyId = partyId, date = today, amount = amount, mode = PaymentMode.CASH,
    )

    private fun alloc(paymentId: String, invoiceId: String, amount: Long) = PaymentAllocationEntity(
        id = "a$paymentId$invoiceId", businessId = "b", createdAt = 0, updatedAt = 0, paymentId = paymentId, invoiceId = invoiceId, amount = amount,
    )

    @Test fun billsLandInTheirAgeBucketsUsingTheUnpaidBalance() {
        val r = CollectionsCalc.compute(
            listOf(party("a")),
            listOf(sale("i1", "a", 1_000, 10), sale("i2", "a", 2_000, 45, paid = 500, status = InvoiceStatus.PARTIAL), sale("i3", "a", 3_000, 200)),
            emptyList(), emptyList(), today,
        )
        val b = r.rows.single().aging.buckets
        assertEquals(1_000, b.getValue(AgeBucket.DAYS_0_30))
        assertEquals(1_500, b.getValue(AgeBucket.DAYS_31_60))
        assertEquals(3_000, b.getValue(AgeBucket.DAYS_90_PLUS))
        assertEquals(5_500, r.grandTotal)
    }

    @Test fun paidAndCancelledBillsAreIgnored() {
        val r = CollectionsCalc.compute(
            listOf(party("a")),
            listOf(sale("p", "a", 1_000, 5, paid = 1_000, status = InvoiceStatus.PAID), sale("c", "a", 1_000, 5, status = InvoiceStatus.CANCELLED)),
            emptyList(), emptyList(), today,
        )
        assertEquals(0, r.rows.size)
    }

    @Test fun suppliersAreNotCollectionRows() {
        val r = CollectionsCalc.compute(listOf(party("s", opening = 5_000, kind = PartyType.SUPPLIER)), emptyList(), emptyList(), emptyList(), today)
        assertEquals(0, r.rows.size)
    }

    @Test fun openingBalanceIsTheOldestMoney() {
        val r = CollectionsCalc.compute(listOf(party("a", opening = 4_000)), emptyList(), emptyList(), emptyList(), today)
        assertEquals(4_000, r.rows.single().aging.buckets.getValue(AgeBucket.DAYS_90_PLUS))
    }

    @Test fun paymentOnAccountReducesTheOldestFirst() {
        val r = CollectionsCalc.compute(
            listOf(party("a", opening = 1_000)),
            listOf(sale("i1", "a", 2_000, 5)),
            listOf(pay("p1", "a", 1_500)), emptyList(), today,
        )
        val b = r.rows.single().aging.buckets
        assertEquals(0, b.getValue(AgeBucket.DAYS_90_PLUS))
        assertEquals(1_500, b.getValue(AgeBucket.DAYS_0_30))
    }

    @Test fun allocatedMoneyIsNotCountedTwice() {
        // 1,000 paid against the 2,000 bill: the bill's balance is already 1,000, so the payment is NOT also "on account"
        val r = CollectionsCalc.compute(
            listOf(party("a")),
            listOf(sale("i1", "a", 2_000, 5, paid = 1_000, status = InvoiceStatus.PARTIAL)),
            listOf(pay("p1", "a", 1_000)), listOf(alloc("p1", "i1", 1_000)), today,
        )
        assertEquals(1_000, r.grandTotal)
    }

    @Test fun biggestDebtorFirst() {
        val r = CollectionsCalc.compute(
            listOf(party("small"), party("big")),
            listOf(sale("i1", "small", 100, 5), sale("i2", "big", 9_000, 5)),
            emptyList(), emptyList(), today,
        )
        assertEquals(listOf("big", "small"), r.rows.map { it.party.id })
    }

    @Test fun weOweThemOpeningBalanceOffsetsWhatTheyOwe() {
        val r = CollectionsCalc.compute(listOf(party("a", opening = 500, type = BalanceType.TO_PAY)), listOf(sale("i1", "a", 2_000, 5)), emptyList(), emptyList(), today)
        assertEquals(1_500, r.grandTotal)
    }
}
