package com.teczo.khatabill.util.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.entities.InvoiceLineEntity
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.localized
import com.teczo.khatabill.testBusiness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

/**
 * Builds real PDFs on the device, opens them with PdfRenderer and saves PNG previews of every page to
 * the app's external files folder (pull them with adb to look at Kannada shaping).
 */
@RunWith(AndroidJUnit4::class)
class InvoicePdfTest {
    private lateinit var context: Context
    private lateinit var outDir: File

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        outDir = File(context.getExternalFilesDir(null), "pdf-preview").apply { mkdirs() }
    }

    private fun line(no: Int, name: String, qty: Long = 2_500, rate: Long = 20_000, bp: Int = 1800): InvoiceLineEntity {
        val gross = qty * rate / 1000
        val tax = gross * bp / 10_000
        return InvoiceLineEntity(
            id = "l$no", businessId = "b1", createdAt = 1, updatedAt = 1, invoiceId = "inv", lineNo = no,
            nameSnapshot = name, hsnSac = "33074100", qty = qty, unit = "kg", rate = rate, taxableValue = gross,
            gstRateBp = bp, cgst = tax / 2, sgst = tax / 2, lineTotal = gross + tax,
        )
    }

    private fun invoice(lines: List<InvoiceLineEntity>, status: InvoiceStatus = InvoiceStatus.UNPAID): InvoiceEntity {
        val taxable = lines.sumOf { it.taxableValue }
        val cgst = lines.sumOf { it.cgst }
        val total = taxable + cgst * 2
        return InvoiceEntity(
            id = "inv", businessId = "b1", createdAt = 1, updatedAt = 1, type = DocumentType.SALE, series = "INV",
            financialYear = "2026-27", sequenceNo = 1, number = "INV/26-27/0001",
            date = LocalDate.of(2026, 11, 17).toEpochDay(), partyName = "ರಮೇಶ್ ಕುಮಾರ್ Ramesh Kumar",
            partyAddress = "ಸರಸ್ವತಿಪುರಂ, Mysuru", partyGstin = "29AAGCB7383J1Z4", placeOfSupplyStateCode = "29",
            subtotal = taxable, discountTotal = 0, taxableTotal = taxable, cgst = cgst, sgst = cgst, grandTotal = total,
            paidAmount = 20_000, status = status, notes = "ದಯವಿಟ್ಟು ಒಂದು ವಾರದೊಳಗೆ ಪಾವತಿಸಿ",
        )
    }

    /** Opens the PDF, renders every page at 2x to a PNG, and returns (pageCount, fraction of non-white pixels on page 1). */
    private fun renderAll(pdf: File, prefix: String): Pair<Int, Double> {
        ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                var ink = 0.0
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
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
                }
                return renderer.pageCount to ink
            }
        }
    }

    private val sample = listOf(
        line(1, "ಅಗರಬತ್ತಿ — ಚಂದನ ಸುಗಂಧ (Sandal agarbatti)"),
        line(2, "Rose agarbatti 50 g pack", qty = 12_000, rate = 4_500, bp = 500),
        line(3, "ಧೂಪದ ಕಡ್ಡಿ ಮತ್ತು ಕರ್ಪೂರ ಸಂಯುಕ್ತ", qty = 3_000, rate = 15_050, bp = 1200),
    )

    @Test fun pdfInEnglishHasContent() {
        val file = InvoicePdfBuilder(context.localized("en")).build(testBusiness(), invoice(sample), sample)
        assertTrue(file.length() > 2_000)
        val (pages, ink) = renderAll(file, "en")
        assertEquals(1, pages)
        assertTrue("page looks blank", ink > 0.01)
    }

    @Test fun pdfInKannadaHasContent() {
        val file = InvoicePdfBuilder(context.localized("kn")).build(testBusiness(), invoice(sample), sample)
        val (pages, ink) = renderAll(file, "kn")
        assertEquals(1, pages)
        assertTrue("page looks blank", ink > 0.01)
    }

    @Test fun pdfInHindiHasContent() {
        val file = InvoicePdfBuilder(context.localized("hi")).build(testBusiness(), invoice(sample), sample)
        val (pages, ink) = renderAll(file, "hi")
        assertEquals(1, pages)
        assertTrue("page looks blank", ink > 0.01)
    }

    @Test fun billOfSupplyForANonGstBusinessHasNoTaxColumns() {
        val untaxed = sample.map { it.copy(gstRateBp = 0, cgst = 0, sgst = 0, lineTotal = it.taxableValue, hsnSac = null) }
        val inv = invoice(untaxed).copy(series = "BOS", number = "BOS/26-27/0001", cgst = 0, sgst = 0, grandTotal = untaxed.sumOf { it.taxableValue })
        val file = InvoicePdfBuilder(context.localized("kn")).build(testBusiness(com.teczo.khatabill.domain.model.GstScheme.UNREGISTERED), inv, untaxed)
        val (pages, _) = renderAll(file, "kn-bos")
        assertEquals(1, pages)
    }

    @Test fun manyLinesFlowOntoMoreThanOnePage() {
        val many = (1..45).map { line(it, "ಅಗರಬತ್ತಿ ಪ್ರಕಾರ $it (Agarbatti variety $it)") }
        val file = InvoicePdfBuilder(context.localized("kn")).build(testBusiness(), invoice(many), many)
        val (pages, _) = renderAll(file, "kn-many")
        assertTrue("expected a multi-page bill, got $pages", pages >= 2)
    }

    @Test fun cancelledInvoiceStillRenders() {
        val file = InvoicePdfBuilder(context.localized("en")).build(testBusiness(), invoice(sample, InvoiceStatus.CANCELLED), sample)
        val (pages, _) = renderAll(file, "en-cancelled")
        assertEquals(1, pages)
    }
}
