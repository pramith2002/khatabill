package com.teczo.khatabill.domain.ledger

import com.teczo.khatabill.domain.model.BalanceType
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.domain.model.PaymentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LedgerCalculatorsTest {

    // ---- balance ----

    private fun bal(
        opening: Long = 0, type: BalanceType = BalanceType.TO_COLLECT,
        inv: List<InvoiceFlow> = emptyList(), pay: List<PaymentFlow> = emptyList(),
    ) = BalanceCalculator.balance(opening, type, inv, pay)

    @Test fun nothingMeansZero() = assertEquals(0, bal())

    @Test fun openingToCollectIsPositive() = assertEquals(5_000, bal(opening = 5_000))

    @Test fun openingToPayIsNegative() = assertEquals(-5_000, bal(opening = 5_000, type = BalanceType.TO_PAY))

    @Test fun salesIncreaseWhatTheyOwe() =
        assertEquals(11_800, bal(inv = listOf(InvoiceFlow(DocumentType.SALE, 11_800))))

    @Test fun paymentsReceivedReduceIt() = assertEquals(
        13_800,
        bal(5_000, inv = listOf(InvoiceFlow(DocumentType.SALE, 11_800)), pay = listOf(PaymentFlow(PaymentType.RECEIVED, 3_000))),
    )

    @Test fun purchasesMeanWeOweThem() =
        assertEquals(-9_000, bal(inv = listOf(InvoiceFlow(DocumentType.PURCHASE, 9_000))))

    @Test fun paymentsMadeReduceWhatWeOwe() = assertEquals(
        -4_000,
        bal(inv = listOf(InvoiceFlow(DocumentType.PURCHASE, 9_000)), pay = listOf(PaymentFlow(PaymentType.PAID, 5_000))),
    )

    @Test fun returnsGoTheOtherWay() {
        assertEquals(7_000, bal(inv = listOf(InvoiceFlow(DocumentType.SALE, 10_000), InvoiceFlow(DocumentType.SALE_RETURN, 3_000))))
        assertEquals(-7_000, bal(inv = listOf(InvoiceFlow(DocumentType.PURCHASE, 10_000), InvoiceFlow(DocumentType.PURCHASE_RETURN, 3_000))))
    }

    @Test fun quotationsAndChallansDoNotCount() = assertEquals(
        0,
        bal(inv = listOf(InvoiceFlow(DocumentType.QUOTATION, 9_999), InvoiceFlow(DocumentType.PROFORMA, 1), InvoiceFlow(DocumentType.DELIVERY_CHALLAN, 5))),
    )

    @Test fun anAdvanceMakesTheBalanceNegative() =
        assertEquals(-2_000, bal(pay = listOf(PaymentFlow(PaymentType.RECEIVED, 2_000))))

    @Test fun balanceEqualsInvoicesMinusPayments() {
        val sales = listOf(11_800L, 25_000L, 3_333L).map { InvoiceFlow(DocumentType.SALE, it) }
        val received = listOf(10_000L, 7_777L).map { PaymentFlow(PaymentType.RECEIVED, it) }
        assertEquals(11_800 + 25_000 + 3_333 - 10_000 - 7_777, bal(inv = sales, pay = received))
    }

    // ---- allocation ----

    private fun open(vararg balances: Long) = balances.mapIndexed { i, b -> OpenInvoice("i${i + 1}", b) }

    @Test fun oldestInvoiceIsPaidFirst() {
        val plan = AllocationPlanner.plan(1_500, open(1_000, 2_000, 3_000))
        assertEquals(listOf(Allocation("i1", 1_000), Allocation("i2", 500)), plan.allocations)
        assertEquals(0, plan.onAccount)
    }

    @Test fun exactPaymentClearsOneInvoice() {
        val plan = AllocationPlanner.plan(2_000, open(2_000, 5_000))
        assertEquals(listOf(Allocation("i1", 2_000)), plan.allocations)
    }

    @Test fun overpaymentStaysOnAccount() {
        val plan = AllocationPlanner.plan(10_000, open(1_000, 2_000))
        assertEquals(listOf(Allocation("i1", 1_000), Allocation("i2", 2_000)), plan.allocations)
        assertEquals(7_000, plan.onAccount)
    }

    @Test fun paymentWithNoOpenInvoicesIsAllOnAccount() {
        val plan = AllocationPlanner.plan(4_000, emptyList())
        assertEquals(emptyList<Allocation>(), plan.allocations)
        assertEquals(4_000, plan.onAccount)
    }

    @Test fun chosenInvoiceIsPaidBeforeTheOldestOne() {
        val plan = AllocationPlanner.plan(2_500, open(1_000, 2_000, 3_000), preferredInvoiceId = "i3")
        assertEquals(listOf(Allocation("i3", 2_500)), plan.allocations)
    }

    @Test fun chosenInvoiceThenOldestForTheRest() {
        val plan = AllocationPlanner.plan(3_500, open(1_000, 2_000, 3_000), preferredInvoiceId = "i3")
        assertEquals(listOf(Allocation("i3", 3_000), Allocation("i1", 500)), plan.allocations)
    }

    @Test fun settledInvoicesAreSkipped() {
        val plan = AllocationPlanner.plan(500, open(0, 800))
        assertEquals(listOf(Allocation("i2", 500)), plan.allocations)
    }

    @Test fun allocationsAlwaysAddUp() {
        val plan = AllocationPlanner.plan(7_654, open(1_111, 2_222, 3_333, 4_444))
        assertEquals(7_654, plan.allocations.sumOf { it.amount } + plan.onAccount)
    }

    @Test fun zeroPaymentIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { AllocationPlanner.plan(0, open(100)) }
    }

    // ---- ageing ----

    private val today = 1_000L

    private fun age(opening: Long = 0, credit: Long = 0, vararg inv: Pair<Long, Long>) =
        AgingCalculator.age(today, opening, inv.map { AgingInvoice(it.first, today - it.second) }, credit).buckets

    @Test fun boundariesFallInTheRightBuckets() {
        assertEquals(AgeBucket.DAYS_0_30, AgingCalculator.bucketFor(0))
        assertEquals(AgeBucket.DAYS_0_30, AgingCalculator.bucketFor(30))
        assertEquals(AgeBucket.DAYS_31_60, AgingCalculator.bucketFor(31))
        assertEquals(AgeBucket.DAYS_31_60, AgingCalculator.bucketFor(60))
        assertEquals(AgeBucket.DAYS_61_90, AgingCalculator.bucketFor(61))
        assertEquals(AgeBucket.DAYS_61_90, AgingCalculator.bucketFor(90))
        assertEquals(AgeBucket.DAYS_90_PLUS, AgingCalculator.bucketFor(91))
    }

    @Test fun invoicesLandInTheirBuckets() {
        val b = age(inv = arrayOf(100L to 5L, 200L to 45L, 300L to 75L, 400L to 200L))
        assertEquals(100, b.getValue(AgeBucket.DAYS_0_30))
        assertEquals(200, b.getValue(AgeBucket.DAYS_31_60))
        assertEquals(300, b.getValue(AgeBucket.DAYS_61_90))
        assertEquals(400, b.getValue(AgeBucket.DAYS_90_PLUS))
    }

    @Test fun openingBalanceIsTheOldestMoney() {
        val b = age(opening = 5_000, inv = arrayOf(100L to 1L))
        assertEquals(5_000, b.getValue(AgeBucket.DAYS_90_PLUS))
    }

    @Test fun creditOnAccountClearsTheOldestFirst() {
        val b = age(opening = 1_000, credit = 1_500, inv = arrayOf(400L to 70L, 900L to 3L))
        // 1,500 credit: clears the 1,000 opening balance, then the 400 invoice, and 100 comes off the newest bucket
        assertEquals(0, b.getValue(AgeBucket.DAYS_90_PLUS))
        assertEquals(0, b.getValue(AgeBucket.DAYS_61_90))
        assertEquals(800, b.getValue(AgeBucket.DAYS_0_30))
    }

    @Test fun creditBiggerThanEverythingLeavesZeroNotNegative() {
        val row = AgingCalculator.age(today, 100, listOf(AgingInvoice(200, today - 5)), 10_000)
        assertEquals(0, row.total)
    }

    @Test fun futureDatedInvoiceCountsAsNew() {
        val b = age(inv = arrayOf(100L to -5L))
        assertEquals(100, b.getValue(AgeBucket.DAYS_0_30))
    }

    @Test fun totalIsTheSumOfBuckets() {
        val row = AgingCalculator.age(today, 50, listOf(AgingInvoice(100, today - 5), AgingInvoice(200, today - 95)), 0)
        assertEquals(350, row.total)
    }

    // ---- invoice status ----

    @Test fun statusFollowsPayments() {
        assertEquals(InvoiceStatus.UNPAID, InvoiceStatusRules.statusFor(1_000, 0))
        assertEquals(InvoiceStatus.PARTIAL, InvoiceStatusRules.statusFor(1_000, 400))
        assertEquals(InvoiceStatus.PAID, InvoiceStatusRules.statusFor(1_000, 1_000))
    }
}
