package com.teczo.khatabill.data.repositories

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.teczo.khatabill.data.db.KhataBillDatabase
import com.teczo.khatabill.data.entities.PartyEntity
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.domain.model.PaymentMode
import com.teczo.khatabill.domain.model.PaymentType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PaymentRepositoryTest {
    private lateinit var db: KhataBillDatabase
    private lateinit var invoices: InvoiceRepository
    private lateinit var payments: PaymentRepository

    private val biz = "biz-1"
    private val day = LocalDate.of(2026, 11, 17).toEpochDay()

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KhataBillDatabase::class.java).build()
        invoices = InvoiceRepository(db, db.invoiceDao(), db.stockDao(), db.paymentDao())
        payments = PaymentRepository(db, db.paymentDao(), db.invoiceDao())
        db.partyDao().upsert(PartyEntity(id = "p1", businessId = biz, createdAt = 1, updatedAt = 1, name = "Lakshmi Stores"))
    }

    @After
    fun tearDown() = db.close()

    private suspend fun sale(
        total: Long, date: Long = day, paid: Long = 0, type: DocumentType = DocumentType.SALE, series: String = "INV",
        partyId: String? = "p1",
    ) =
        invoices.createInvoice(
            InvoiceDraft(
                businessId = biz, type = type, series = series, prefix = "", financialYearStartMonth = 4, date = date, dueDate = null,
                partyId = partyId, partyName = partyId?.let { "Lakshmi Stores" }, partyGstin = null, partyAddress = null, partyStateCode = null,
                placeOfSupplyStateCode = "29", subtotal = total, discountTotal = 0, otherCharges = 0, taxableTotal = total,
                cgst = 0, sgst = 0, igst = 0, roundOff = 0, grandTotal = total, paidAmount = paid, paymentMode = PaymentMode.UPI,
                notes = null, terms = null, transportDetails = null, vehicleNo = null,
                lines = listOf(
                    InvoiceLineDraft(
                        itemId = null, nameSnapshot = "Agarbatti", hsnSac = null, qty = 1_000, unit = "kg", rate = total,
                        taxInclusive = false, discountPercentBp = 0, discountAmount = 0, taxableValue = total, gstRateBp = 0,
                        cgst = 0, sgst = 0, igst = 0, lineTotal = total, affectsStock = false,
                    ),
                ),
            ),
        )

    private suspend fun receive(amount: Long, preferred: String? = null) = payments.record(
        biz, PaymentType.RECEIVED, "p1", day, amount, PaymentMode.CASH, null, null, preferred,
    )

    private suspend fun paidOf(id: String) = db.invoiceDao().getById(id)!!.paidAmount
    private suspend fun statusOf(id: String) = db.invoiceDao().getById(id)!!.status

    @Test fun paymentClearsTheOldestInvoiceFirst() = runBlocking {
        val old = sale(1_000, day - 40)
        val newer = sale(2_000, day - 5)
        receive(1_500)
        assertEquals(1_000, paidOf(old.id))
        assertEquals(InvoiceStatus.PAID, statusOf(old.id))
        assertEquals(500, paidOf(newer.id))
        assertEquals(InvoiceStatus.PARTIAL, statusOf(newer.id))
    }

    @Test fun partialPaymentsAddUpAcrossSeveralPayments() = runBlocking {
        val inv = sale(1_000)
        receive(300)
        receive(300)
        assertEquals(600, paidOf(inv.id))
        assertEquals(InvoiceStatus.PARTIAL, statusOf(inv.id))
        receive(400)
        assertEquals(InvoiceStatus.PAID, statusOf(inv.id))
    }

    @Test fun overpaymentLeavesTheInvoicePaidAndKeepsTheRestOnAccount() = runBlocking {
        val inv = sale(1_000)
        val p = receive(1_800)
        assertEquals(1_000, paidOf(inv.id))
        val allocated = db.paymentDao().getAllocationsOfPayment(p.id).sumOf { it.amount }
        assertEquals(1_000, allocated)
        assertEquals(1_800, db.paymentDao().getById(p.id)!!.amount)
    }

    @Test fun aChosenInvoiceIsPaidFirst() = runBlocking {
        val old = sale(1_000, day - 40)
        val chosen = sale(2_000, day - 5)
        receive(1_500, preferred = chosen.id)
        assertEquals(1_500, paidOf(chosen.id))
        assertEquals(0, paidOf(old.id))
    }

    @Test fun purchasePaymentsAreAllocatedToPurchaseInvoices() = runBlocking {
        val bill = sale(5_000, type = DocumentType.PURCHASE, series = "PUR")
        val salesInvoice = sale(5_000)
        payments.record(biz, PaymentType.PAID, "p1", day, 2_000, PaymentMode.BANK, "UTR123", null)
        assertEquals(2_000, paidOf(bill.id))
        assertEquals(0, paidOf(salesInvoice.id))
    }

    @Test fun deletingAPaymentGivesTheBalanceBack() = runBlocking {
        val inv = sale(1_000)
        val p = receive(1_000)
        assertEquals(InvoiceStatus.PAID, statusOf(inv.id))
        payments.delete(p.id)
        assertEquals(0, paidOf(inv.id))
        assertEquals(InvoiceStatus.UNPAID, statusOf(inv.id))
        assertTrue(db.paymentDao().observeAll(biz).first().isEmpty())
    }

    @Test fun moneyTakenAtBillingIsAPaymentWithAnAllocation() = runBlocking {
        val inv = sale(1_000, paid = 400)
        val list = db.paymentDao().observeAll(biz).first()
        assertEquals(1, list.size)
        assertEquals(PaymentType.RECEIVED, list.single().type)
        assertEquals(PaymentMode.UPI, list.single().mode)
        assertEquals(inv.id, list.single().invoiceId)
        assertEquals(400, db.paymentDao().getAllocationsOfInvoice(inv.id).sumOf { it.amount })
        assertEquals(InvoiceStatus.PARTIAL, statusOf(inv.id))
    }

    @Test fun laterPaymentsContinueFromWhatWasPaidAtBilling() = runBlocking {
        val inv = sale(1_000, paid = 400)
        receive(600)
        assertEquals(1_000, paidOf(inv.id))
        assertEquals(InvoiceStatus.PAID, statusOf(inv.id))
    }

    @Test fun cancellingRefundsBillingTimeMoneyButKeepsLaterPaymentsOnAccount() = runBlocking {
        val inv = sale(1_000, paid = 400)
        val later = receive(300)
        invoices.cancelInvoice(inv.id, "mistake")
        val remaining = db.paymentDao().observeAll(biz).first()
        assertEquals(listOf(later.id), remaining.map { it.id })
        assertTrue(db.paymentDao().getAllocationsOfInvoice(inv.id).isEmpty())
    }

    @Test fun aWalkInSaleWithNoPartyCanBePaidAtBilling() = runBlocking {
        val inv = sale(1_000, paid = 1_000, partyId = null)
        val list = db.paymentDao().observeAll(biz).first()
        assertEquals(null, list.single().partyId)
        assertEquals(InvoiceStatus.PAID, statusOf(inv.id))
    }
}
