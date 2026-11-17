package com.teczo.khatabill.util.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.teczo.khatabill.R
import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.entities.InvoiceLineEntity
import com.teczo.khatabill.data.entities.PartyEntity
import com.teczo.khatabill.domain.ledger.Statement
import com.teczo.khatabill.domain.ledger.StatementBill
import com.teczo.khatabill.domain.ledger.StatementBuilder
import com.teczo.khatabill.domain.ledger.StatementPayment
import com.teczo.khatabill.domain.model.BalanceType
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.domain.model.PaymentMode
import com.teczo.khatabill.domain.model.PaymentType
import com.teczo.khatabill.domain.report.Reports
import com.teczo.khatabill.localized
import com.teczo.khatabill.testBusiness
import com.teczo.khatabill.util.export.CsvExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

/** Builds the statement PDF, a GST report PDF and a CSV on the device, and renders the PDFs to PNG for review. */
@RunWith(AndroidJUnit4::class)
class ExportsTest {
    private lateinit var context: Context
    private lateinit var outDir: File
    private val day = LocalDate.of(2026, 11, 17).toEpochDay()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        outDir = File(context.getExternalFilesDir(null), "export-preview").apply { mkdirs() }
    }

    private fun render(pdf: File, prefix: String): Pair<Int, Double> {
        ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { r ->
                var ink = 0.0
                for (i in 0 until r.pageCount) r.openPage(i).use { page ->
                    val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    FileOutputStream(File(outDir, "$prefix-p${i + 1}.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    if (i == 0) {
                        val px = IntArray(bmp.width * bmp.height)
                        bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
                        ink = px.count { it != Color.WHITE }.toDouble() / px.size
                    }
                    bmp.recycle()
                }
                return r.pageCount to ink
            }
        }
    }

    private fun sale(i: Int, total: Long, gstin: String?, intra: Boolean = true): Pair<InvoiceEntity, InvoiceLineEntity> {
        val tax = total * 18 / 100
        val half = tax / 2
        val inv = InvoiceEntity(
            id = "inv$i", businessId = "b1", createdAt = i.toLong(), updatedAt = 0, type = DocumentType.SALE, series = "INV", financialYear = "2026-27",
            sequenceNo = i, number = "SGINV/26-27/%04d".format(i), date = day - 20 + i, partyId = "p$i", partyName = if (i % 2 == 0) "ರಮೇಶ್ ಕುಮಾರ್ $i" else "Party $i",
            partyGstin = gstin, placeOfSupplyStateCode = if (intra) "29" else "27", subtotal = total, discountTotal = 0, taxableTotal = total,
            cgst = if (intra) half else 0, sgst = if (intra) half else 0, igst = if (intra) 0 else tax, grandTotal = total + tax, status = InvoiceStatus.UNPAID,
        )
        val line = InvoiceLineEntity(
            id = "l$i", businessId = "b1", createdAt = 0, updatedAt = 0, invoiceId = inv.id, lineNo = 1, nameSnapshot = "ಅಗರಬತ್ತಿ Agarbatti", hsnSac = "33074100",
            qty = 2_000, unit = "kg", rate = total / 2, taxableValue = total, gstRateBp = 1800, cgst = inv.cgst, sgst = inv.sgst, igst = inv.igst, lineTotal = inv.grandTotal,
        )
        return inv to line
    }

    @Test fun statementPdfInKannadaHasTheRowsAndRendersInk() {
        val st: Statement = StatementBuilder.build(
            400_000, BalanceType.TO_COLLECT,
            listOf(StatementBill(day - 30, 1, "SGINV/26-27/0001", DocumentType.SALE, 590_000), StatementBill(day - 3, 3, "SGINV/26-27/0002", DocumentType.SALE, 141_750)),
            listOf(StatementPayment(day - 10, 2, PaymentType.RECEIVED, 300_000, PaymentMode.UPI)),
        )
        val party = PartyEntity(id = "pp", businessId = "b1", createdAt = 0, updatedAt = 0, name = "Lakshmi Stores", nameKn = "ಲಕ್ಷ್ಮಿ ಸ್ಟೋರ್ಸ್", phone = "9845012345")
        val file = StatementPdfBuilder(context.localized("kn")).build(testBusiness(), party, st)
        val (pages, ink) = render(file, "statement-kn")
        assertEquals(1, pages)
        assertTrue("statement looks blank", ink > 0.01)
        assertEquals(831_750, st.closing) // 4,000 + 5,900 + 1,417.50 - 3,000 = 8,317.50
    }

    @Test fun gstReportPdfListsB2bAndHsnAndRendersInk() {
        val pairs = (1..24).map { sale(it, 100_000L * it, if (it <= 12) "29AAGCB7383J1Z4" else null, intra = it % 2 == 1) }
        val invoices = pairs.map { it.first }
        val lines = pairs.map { it.second }
        val tables = listOf(Reports.gstr1B2b(invoices), Reports.gstr1B2c(invoices, lines), Reports.gstr1Hsn(invoices, lines))
        val file = ReportPdfBuilder(context.localized("en")).build(
            testBusiness(), R.string.report_gstr1, "01/11/2026 – 30/11/2026", tables, R.string.report_gst_note, File(File(context.cacheDir, "invoices"), "gstr1-test.pdf"),
        )
        val (pages, ink) = render(file, "gstr1-en")
        assertTrue("expected at least one page, got $pages", pages >= 1)
        assertTrue("report looks blank", ink > 0.01)
    }

    @Test fun gstReportPdfInKannadaToo() {
        val (inv, line) = sale(1, 250_000, "29AAGCB7383J1Z4")
        val file = ReportPdfBuilder(context.localized("kn")).build(
            testBusiness(), R.string.report_gstr1, null, listOf(Reports.gstr1B2b(listOf(inv)), Reports.gstr1Hsn(listOf(inv), listOf(line))), R.string.report_gst_note,
            File(File(context.cacheDir, "invoices"), "gstr1-kn.pdf"),
        )
        val (_, ink) = render(file, "gstr1-kn")
        assertTrue(ink > 0.01)
    }

    @Test fun csvFileStartsWithABomSoExcelReadsKannadaAndHasPlainNumbers() {
        val (inv, _) = sale(2, 123_456, "29AAGCB7383J1Z4")
        val file = CsvExport.write(
            File(File(context.cacheDir, "invoices"), "sales-test.csv"), listOf(Reports.sales(listOf(inv), "Walk-in")),
            { context.localized("kn").getString(it) }, listOf("Sri Ganesh Agarbatti"),
        )
        val bytes = file.readBytes()
        assertEquals(listOf(0xEF, 0xBB, 0xBF), bytes.take(3).map { it.toInt() and 0xFF })
        val text = file.readText(Charsets.UTF_8)
        assertTrue(text.contains("ರಮೇಶ್ ಕುಮಾರ್ 2"))
        assertTrue(text.contains("1234.56")) // taxable, no grouping
        assertTrue(!text.contains("1,234.56"))
        file.copyTo(File(outDir, "sales-kn.csv"), overwrite = true)
    }
}
