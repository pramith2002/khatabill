package com.teczo.khatabill.data.repositories

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.teczo.khatabill.data.db.KhataBillDatabase
import com.teczo.khatabill.data.entities.ItemEntity
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.domain.model.StockReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class InvoiceRepositoryTest {
    private lateinit var db: KhataBillDatabase
    private lateinit var invoices: InvoiceRepository
    private lateinit var items: ItemRepository

    private val biz = "biz-1"
    private val day = LocalDate.of(2026, 11, 17).toEpochDay()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KhataBillDatabase::class.java).build()
        invoices = InvoiceRepository(db, db.invoiceDao(), db.stockDao(), db.paymentDao())
        items = ItemRepository(db, db.itemDao(), db.stockDao())
    }

    @After
    fun tearDown() = db.close()

    private fun item(id: String, name: String = "Agarbatti") = ItemEntity(
        id = id, businessId = biz, createdAt = 1, updatedAt = 1, name = name, unit = "kg",
    )

    /** One exempt line of 20.00 so totals are trivially consistent. */
    private fun line(itemId: String?, qty: Long = 2_000, affectsStock: Boolean = true) = InvoiceLineDraft(
        itemId = itemId, nameSnapshot = "Agarbatti", hsnSac = null, qty = qty, unit = "kg",
        rate = 1_000, taxInclusive = false, discountPercentBp = 0, discountAmount = 0,
        taxableValue = 2_000, gstRateBp = 0, cgst = 0, sgst = 0, igst = 0, lineTotal = 2_000,
        affectsStock = affectsStock,
    )

    private fun draft(
        type: DocumentType = DocumentType.SALE,
        series: String = "INV",
        date: Long = day,
        lines: List<InvoiceLineDraft> = listOf(line(null)),
        grandTotal: Long = 2_000,
    ) = InvoiceDraft(
        businessId = biz, type = type, series = series, prefix = "", financialYearStartMonth = 4,
        date = date, dueDate = null, partyId = null, partyName = null, partyGstin = null,
        partyAddress = null, partyStateCode = null, placeOfSupplyStateCode = "29",
        subtotal = 2_000, discountTotal = 0, otherCharges = 0, taxableTotal = 2_000,
        cgst = 0, sgst = 0, igst = 0, roundOff = 0, grandTotal = grandTotal, paidAmount = 0,
        notes = null, terms = null, transportDetails = null, vehicleNo = null, lines = lines,
    )

    @Test
    fun numbersAreSequential() = runBlocking {
        val a = invoices.createInvoice(draft())
        val b = invoices.createInvoice(draft())
        assertEquals("INV/26-27/0001", a.number)
        assertEquals("INV/26-27/0002", b.number)
        assertEquals(listOf(1, 2), listOf(a.sequenceNo, b.sequenceNo))
        assertEquals(InvoiceStatus.UNPAID, a.status)
    }

    @Test
    fun cancelledInvoiceNumberIsNeverReused() = runBlocking {
        invoices.createInvoice(draft())
        val second = invoices.createInvoice(draft())
        invoices.cancelInvoice(second.id, "wrong party")
        val third = invoices.createInvoice(draft())
        assertEquals("INV/26-27/0003", third.number)
        val cancelled = db.invoiceDao().getById(second.id)!!
        assertEquals(InvoiceStatus.CANCELLED, cancelled.status)
        assertEquals("wrong party", cancelled.cancelReason)
    }

    @Test
    fun newFinancialYearRestartsNumbering() = runBlocking {
        invoices.createInvoice(draft())
        val april = LocalDate.of(2027, 4, 1).toEpochDay()
        val next = invoices.createInvoice(draft(date = april))
        assertEquals("2027-28", next.financialYear)
        assertEquals("INV/27-28/0001", next.number)
    }

    @Test
    fun eachSeriesHasItsOwnCounter() = runBlocking {
        val inv = invoices.createInvoice(draft(series = "INV"))
        val quote = invoices.createInvoice(draft(type = DocumentType.QUOTATION, series = "QUO"))
        assertEquals(1, inv.sequenceNo)
        assertEquals(1, quote.sequenceNo)
    }

    @Test
    fun saleTakesStockOutInTheSameTransaction() = runBlocking {
        items.create(item("i1"), openingStock = 10_000, date = day)
        val sale = invoices.createInvoice(draft(lines = listOf(line("i1", qty = 2_500))))
        assertEquals(7_500, db.stockDao().getStock("i1"))
        val movement = db.stockDao().getByInvoice(sale.id).single()
        assertEquals(StockReason.SALE, movement.reason)
        assertEquals(-2_500, movement.qtyDelta)
    }

    @Test
    fun purchaseBringsStockIn() = runBlocking {
        items.create(item("i1"), openingStock = 0, date = day)
        invoices.createInvoice(draft(type = DocumentType.PURCHASE, series = "PUR", lines = listOf(line("i1", qty = 4_000))))
        assertEquals(4_000, db.stockDao().getStock("i1"))
    }

    @Test
    fun saleReturnBringsStockBack() = runBlocking {
        items.create(item("i1"), openingStock = 5_000, date = day)
        invoices.createInvoice(draft(type = DocumentType.SALE_RETURN, series = "CN", lines = listOf(line("i1", qty = 1_000))))
        assertEquals(6_000, db.stockDao().getStock("i1"))
    }

    @Test
    fun cancellingPutsStockBack() = runBlocking {
        items.create(item("i1"), openingStock = 10_000, date = day)
        val sale = invoices.createInvoice(draft(lines = listOf(line("i1", qty = 3_000))))
        assertEquals(7_000, db.stockDao().getStock("i1"))
        invoices.cancelInvoice(sale.id, "customer returned")
        assertEquals(10_000, db.stockDao().getStock("i1"))
    }

    @Test
    fun cancellingTwiceIsRejected() = runBlocking {
        val sale = invoices.createInvoice(draft())
        invoices.cancelInvoice(sale.id, "x")
        assertThrows(IllegalArgumentException::class.java) { runBlocking { invoices.cancelInvoice(sale.id, "again") } }
        Unit
    }

    @Test
    fun quotationDoesNotTouchStock() = runBlocking {
        items.create(item("i1"), openingStock = 10_000, date = day)
        invoices.createInvoice(draft(type = DocumentType.QUOTATION, series = "QUO", lines = listOf(line("i1", qty = 3_000))))
        assertEquals(10_000, db.stockDao().getStock("i1"))
    }

    @Test
    fun serviceLinesDoNotTouchStock() = runBlocking {
        items.create(item("svc"), openingStock = 10_000, date = day)
        invoices.createInvoice(draft(lines = listOf(line("svc", qty = 3_000, affectsStock = false))))
        assertEquals(10_000, db.stockDao().getStock("svc"))
    }

    @Test
    fun failedSaveRollsBackNumberInvoiceAndStock() = runBlocking {
        items.create(item("i1"), openingStock = 10_000, date = day)
        // second line points at an item that does not exist, so the insert fails mid-transaction
        val bad = draft(lines = listOf(line("i1", qty = 1_000), line("missing", qty = 1_000))).copy(
            subtotal = 4_000, taxableTotal = 4_000, grandTotal = 4_000,
        )
        assertThrows(Exception::class.java) { runBlocking { invoices.createInvoice(bad) } }

        assertTrue(db.invoiceDao().observeByType(biz, DocumentType.SALE).first().isEmpty())
        assertEquals(10_000, db.stockDao().getStock("i1"))
        val next = invoices.createInvoice(draft())
        assertEquals(1, next.sequenceNo)
    }

    @Test
    fun totalsThatDoNotAddUpAreRefusedAndNothingIsSaved() = runBlocking {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { invoices.createInvoice(draft(grandTotal = 2_500)) }
        }
        assertTrue(db.invoiceDao().observeByType(biz, DocumentType.SALE).first().isEmpty())
    }

    @Test
    fun oldInvoiceLinesKeepTheirSnapshotWhenTheItemIsEdited() = runBlocking {
        val original = item("i1", name = "Agarbatti")
        items.create(original, openingStock = 10_000, date = day)
        val sale = invoices.createInvoice(draft(lines = listOf(line("i1"))))
        items.update(original.copy(name = "Renamed item"))
        val saved = db.invoiceDao().getLines(sale.id).single()
        assertEquals("Agarbatti", saved.nameSnapshot)
    }

    @Test
    fun paidAmountDecidesStatus() = runBlocking {
        val paid = invoices.createInvoice(draft().copy(paidAmount = 2_000))
        val partial = invoices.createInvoice(draft().copy(paidAmount = 500))
        assertEquals(InvoiceStatus.PAID, paid.status)
        assertEquals(InvoiceStatus.PARTIAL, partial.status)
    }

    @Test
    fun openingStockCreatesAnOpeningMovement() = runBlocking {
        items.create(item("i1"), openingStock = 12_500, date = day)
        val movement = db.stockDao().observeMovements("i1").first().single()
        assertEquals(StockReason.OPENING, movement.reason)
        assertEquals(12_500, db.stockDao().getStock("i1"))
    }
}
