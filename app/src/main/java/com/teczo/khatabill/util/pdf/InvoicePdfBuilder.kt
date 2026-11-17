package com.teczo.khatabill.util.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.teczo.khatabill.R
import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.entities.InvoiceLineEntity
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.util.qr.QrCode
import com.teczo.khatabill.util.upi.UpiLink
import com.teczo.khatabill.ui.util.Labels
import com.teczo.khatabill.util.dates.DateFormat
import com.teczo.khatabill.util.money.AmountInWords
import com.teczo.khatabill.util.money.MoneyFormatter
import com.teczo.khatabill.util.money.MoneyInput
import java.io.File

/**
 * Draws an A4 invoice with Android's PdfDocument. Text is laid out with StaticLayout, which shapes
 * Kannada and Devanagari conjuncts correctly. Each piece of text uses the bundled Noto font for its
 * script, so Kannada item names print correctly even when the rest of the bill is in English.
 *
 * Pass a Context whose resources use the language the bill should be printed in.
 */
class InvoicePdfBuilder(private val context: Context) {

    private val kannada: Typeface? by lazy { ResourcesCompat.getFont(context, R.font.noto_sans_kannada) }
    private val devanagari: Typeface? by lazy { ResourcesCompat.getFont(context, R.font.noto_sans_devanagari) }

    fun build(business: BusinessEntity, invoice: InvoiceEntity, lines: List<InvoiceLineEntity>): File {
        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val file = File(dir, invoice.number.replace('/', '-') + ".pdf")
        val doc = PdfDocument()
        try {
            Renderer(doc, business, invoice, lines).render()
            file.outputStream().use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
        return file
    }

    private fun faceFor(text: String): Typeface? {
        for (ch in text) {
            when (ch.code) {
                in 0x0C80..0x0CFF -> return kannada
                in 0x0900..0x097F -> return devanagari
            }
        }
        return null
    }

    private fun paint(text: String, size: Float, bold: Boolean, color: Int): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            isFakeBoldText = bold
            this.color = color
            faceFor(text)?.let { typeface = it }
        }

    private fun layout(text: String, width: Float, size: Float, bold: Boolean, align: Layout.Alignment, color: Int): StaticLayout {
        val p = paint(text, size, bold, color)
        return StaticLayout.Builder.obtain(text, 0, text.length, p, width.toInt().coerceAtLeast(1))
            .setAlignment(align)
            .setLineSpacing(0f, 1.15f)
            .setIncludePad(false)
            .build()
    }

    private fun unitText(code: String): String = Labels.unit(code)?.let { context.getString(it) } ?: code
    private fun stateText(code: String): String = Labels.state(code)?.let { context.getString(it) } ?: code
    private fun money(paise: Long) = MoneyFormatter.format(paise)

    private inner class Renderer(
        val doc: PdfDocument,
        val business: BusinessEntity,
        val invoice: InvoiceEntity,
        val lines: List<InvoiceLineEntity>,
    ) {
        private val pageW = 595
        private val pageH = 842
        private val margin = 32f
        private val contentW = pageW - 2 * margin
        private val bottomLimit = pageH - 56f

        private var pageNo = 0
        private lateinit var page: PdfDocument.Page
        private lateinit var canvas: Canvas
        private var y = 0f

        private val showTax = lines.any { it.gstRateBp > 0 } || invoice.cgst + invoice.sgst + invoice.igst > 0

        // Column widths add up to contentW (531). Tax layout and plain layout.
        private val widths: FloatArray =
            if (showTax) floatArrayOf(18f, 150f, 52f, 52f, 54f, 42f, 60f, 34f, 69f)
            else floatArrayOf(20f, 231f, 65f, 70f, 65f, 80f)
        private val headers: List<String> =
            if (showTax) listOf(
                "#", context.getString(R.string.pdf_col_item), context.getString(R.string.pdf_col_hsn),
                context.getString(R.string.pdf_col_qty), context.getString(R.string.pdf_col_rate),
                context.getString(R.string.pdf_col_discount), context.getString(R.string.pdf_col_taxable),
                context.getString(R.string.pdf_col_gst), context.getString(R.string.pdf_col_amount),
            ) else listOf(
                "#", context.getString(R.string.pdf_col_item), context.getString(R.string.pdf_col_qty),
                context.getString(R.string.pdf_col_rate), context.getString(R.string.pdf_col_discount),
                context.getString(R.string.pdf_col_amount),
            )

        fun render() {
            newPage()
            drawHeader()
            drawBillTo()
            drawTableHeader()
            lines.forEachIndexed { i, line -> drawRow(i + 1, line) }
            drawTotals()
            drawFooterBlocks()
            finishPage()
        }

        private fun newPage() {
            pageNo += 1
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
            canvas = page.canvas
            y = margin
        }

        private fun finishPage() {
            if (invoice.status == InvoiceStatus.CANCELLED) drawWatermark()
            val thanks = context.getString(R.string.pdf_thanks)
            val pageText = context.getString(R.string.pdf_page, pageNo)
            draw(thanks, margin, pageH - 40f, contentW / 2, 8f, align = Layout.Alignment.ALIGN_NORMAL, color = Color.DKGRAY)
            draw(pageText, margin + contentW / 2, pageH - 40f, contentW / 2, 8f, align = Layout.Alignment.ALIGN_OPPOSITE, color = Color.DKGRAY)
            doc.finishPage(page)
        }

        private fun drawWatermark() {
            val text = context.getString(R.string.status_cancelled).uppercase()
            val p = paint(text, 64f, true, Color.argb(60, 200, 0, 0))
            canvas.save()
            canvas.rotate(-30f, pageW / 2f, pageH / 2f)
            canvas.drawText(text, pageW / 2f - p.measureText(text) / 2, pageH / 2f, p)
            canvas.restore()
        }

        /** Draws text with its top-left at (x, y) and returns the height used. */
        private fun draw(
            text: String, x: Float, top: Float, width: Float, size: Float,
            bold: Boolean = false, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL, color: Int = Color.BLACK,
        ): Float {
            if (text.isEmpty()) return 0f
            val l = layout(text, width, size, bold, align, color)
            canvas.save()
            canvas.translate(x, top)
            l.draw(canvas)
            canvas.restore()
            return l.height.toFloat()
        }

        private fun height(text: String, width: Float, size: Float, bold: Boolean = false): Float =
            if (text.isEmpty()) 0f else layout(text, width, size, bold, Layout.Alignment.ALIGN_NORMAL, Color.BLACK).height.toFloat()

        private fun ensure(h: Float, redrawTableHeader: Boolean = false) {
            if (y + h <= bottomLimit) return
            finishPage()
            newPage()
            if (redrawTableHeader) drawTableHeader()
        }

        private fun drawHeader() {
            val leftW = 330f
            val rightX = margin + 346f
            val rightW = contentW - 346f
            var ly = y
            ly += draw(business.name, margin, ly, leftW, 16f, bold = true)
            val place = listOfNotNull(business.address, business.city, stateText(business.stateCode), business.pincode)
                .filter { it.isNotBlank() }.joinToString(", ")
            ly += draw(place, margin, ly + 2, leftW, 9f) + 2
            business.gstin?.let { ly += draw(context.getString(R.string.pdf_gstin, it), margin, ly + 2, leftW, 9f, bold = true) + 2 }
            ly += draw(context.getString(R.string.pdf_phone, business.phone), margin, ly + 2, leftW, 9f) + 2

            var ry = y
            ry += draw(context.getString(Labels.docTitle(invoice.type, invoice.series)), rightX, ry, rightW, 14f, bold = true, align = Layout.Alignment.ALIGN_OPPOSITE)
            ry += draw(context.getString(R.string.invoice_number) + ": " + invoice.number, rightX, ry + 4, rightW, 10f, align = Layout.Alignment.ALIGN_OPPOSITE) + 4
            ry += draw(context.getString(R.string.pdf_date, DateFormat.format(invoice.date)), rightX, ry + 2, rightW, 10f, align = Layout.Alignment.ALIGN_OPPOSITE) + 2

            y = maxOf(ly, ry) + 8
            rule()
        }

        private fun rule() {
            val p = Paint().apply { color = Color.GRAY; strokeWidth = 0.7f }
            canvas.drawLine(margin, y, margin + contentW, y, p)
            y += 8
        }

        private fun drawBillTo() {
            val name = invoice.partyName ?: return
            y += draw(context.getString(R.string.invoice_bill_to), margin, y, contentW, 8f, bold = true, color = Color.DKGRAY)
            y += draw(name, margin, y + 1, contentW, 11f, bold = true) + 1
            invoice.partyAddress?.takeIf { it.isNotBlank() }?.let { y += draw(it, margin, y + 1, contentW, 9f) + 1 }
            invoice.partyGstin?.takeIf { it.isNotBlank() }?.let { y += draw(context.getString(R.string.pdf_gstin, it), margin, y + 1, contentW, 9f) + 1 }
            if (showTax && invoice.placeOfSupplyStateCode.isNotBlank()) {
                y += draw(context.getString(R.string.pdf_place_of_supply, stateText(invoice.placeOfSupplyStateCode)), margin, y + 1, contentW, 9f) + 1
            }
            y += 8
        }

        private fun columnX(index: Int): Float = margin + widths.take(index).sum()

        private fun drawTableHeader() {
            val h = 18f
            ensure(h + 20f)
            val fill = Paint().apply { color = Color.rgb(238, 238, 238) }
            canvas.drawRect(margin, y, margin + contentW, y + h, fill)
            headers.forEachIndexed { i, text ->
                val rightAlign = i >= if (showTax) 3 else 2
                draw(
                    text, columnX(i) + 2f, y + 4f, widths[i] - 4f, 8f, bold = true,
                    align = if (rightAlign) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL,
                )
            }
            y += h + 2
        }

        private fun drawRow(no: Int, line: InvoiceLineEntity) {
            val nameCol = 1
            val nameW = widths[nameCol] - 4f
            val nameH = height(line.nameSnapshot, nameW, 9f)
            val rowH = maxOf(nameH, 11f) + 6f
            ensure(rowH, redrawTableHeader = true)

            val qty = MoneyInput.qtyToText(line.qty) + " " + unitText(line.unit)
            val pct = if (line.gstRateBp > 0) context.getString(R.string.percent_value, MoneyInput.bpToText(line.gstRateBp)) else "-"
            val cells: List<String> = if (showTax) listOf(
                no.toString(), line.nameSnapshot, line.hsnSac.orEmpty(), qty, money(line.rate),
                if (line.discountAmount > 0) money(line.discountAmount) else "-",
                money(line.taxableValue), pct, money(line.lineTotal),
            ) else listOf(
                no.toString(), line.nameSnapshot, qty, money(line.rate),
                if (line.discountAmount > 0) money(line.discountAmount) else "-", money(line.lineTotal),
            )
            cells.forEachIndexed { i, text ->
                val rightAlign = i >= if (showTax) 3 else 2
                draw(
                    text, columnX(i) + 2f, y + 3f, widths[i] - 4f, 9f,
                    align = if (rightAlign) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL,
                )
            }
            y += rowH
            val p = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }
            canvas.drawLine(margin, y - 1, margin + contentW, y - 1, p)
        }

        private fun totalRow(label: String, value: String, bold: Boolean = false) {
            val x = margin + contentW - 210f
            val h = height(label, 100f, 9f, bold).coerceAtLeast(12f) + 3f
            ensure(h)
            draw(label, x, y, 110f, if (bold) 11f else 9f, bold = bold)
            draw(value, x + 110f, y, 100f, if (bold) 11f else 9f, bold = bold, align = Layout.Alignment.ALIGN_OPPOSITE)
            y += h
        }

        private fun drawTotals() {
            y += 8
            ensure(150f)
            totalRow(context.getString(R.string.total_subtotal), money(invoice.subtotal))
            if (invoice.discountTotal > 0) totalRow(context.getString(R.string.total_discount), "- " + money(invoice.discountTotal))
            totalRow(context.getString(R.string.total_taxable), money(invoice.taxableTotal))
            if (invoice.cgst > 0) totalRow(context.getString(R.string.total_cgst), money(invoice.cgst))
            if (invoice.sgst > 0) totalRow(context.getString(R.string.total_sgst), money(invoice.sgst))
            if (invoice.igst > 0) totalRow(context.getString(R.string.total_igst), money(invoice.igst))
            if (invoice.otherCharges > 0) totalRow(context.getString(R.string.total_other), money(invoice.otherCharges))
            if (invoice.roundOff != 0L) {
                totalRow(context.getString(R.string.total_roundoff), (if (invoice.roundOff < 0) "- " else "+ ") + money(kotlin.math.abs(invoice.roundOff)))
            }
            totalRow(context.getString(R.string.total_grand), "₹" + money(invoice.grandTotal), bold = true)
            if (invoice.paidAmount > 0) {
                totalRow(context.getString(R.string.total_paid), "₹" + money(invoice.paidAmount))
                totalRow(context.getString(R.string.total_due), "₹" + money(invoice.grandTotal - invoice.paidAmount))
            }
        }

        private fun drawFooterBlocks() {
            y += 8
            ensure(110f)
            val label = context.getString(R.string.pdf_amount_in_words)
            y += draw(label, margin, y, contentW, 8f, bold = true, color = Color.DKGRAY)
            y += draw(AmountInWords.english(invoice.grandTotal), margin, y + 1, contentW, 9f) + 12

            val hasBank = !business.bankName.isNullOrBlank() || !business.accountNo.isNullOrBlank() || !business.ifsc.isNullOrBlank()
            if (hasBank || !business.upiId.isNullOrBlank()) {
                y += draw(context.getString(R.string.pdf_bank_details), margin, y, contentW, 8f, bold = true, color = Color.DKGRAY)
                business.bankName?.takeIf { it.isNotBlank() }?.let { y += draw(context.getString(R.string.pdf_bank_name, it), margin, y + 1, contentW, 9f) + 1 }
                business.accountNo?.takeIf { it.isNotBlank() }?.let { y += draw(context.getString(R.string.pdf_account_no, it), margin, y + 1, contentW, 9f) + 1 }
                business.ifsc?.takeIf { it.isNotBlank() }?.let { y += draw(context.getString(R.string.pdf_ifsc, it), margin, y + 1, contentW, 9f) + 1 }
                business.upiId?.takeIf { it.isNotBlank() }?.let { y += draw(context.getString(R.string.pdf_upi, it), margin, y + 1, contentW, 9f) + 1 }
                y += 12
            }
            invoice.notes?.takeIf { it.isNotBlank() }?.let { y += draw(context.getString(R.string.pdf_notes, it), margin, y, contentW, 9f) + 6 }

            // Scan-to-pay QR for the unpaid balance of a live sale (the app only builds the link; payment is confirmed by hand).
            val due = invoice.grandTotal - invoice.paidAmount
            val upi = business.upiId
            if (invoice.type == DocumentType.SALE && invoice.status != InvoiceStatus.CANCELLED && due > 0 && !upi.isNullOrBlank()) {
                ensure(104f)
                val modules = QrCode.modules(UpiLink.build(upi, business.name, due, invoice.number))
                val size = 88f
                val cell = size / modules.size
                val dark = Paint().apply { color = Color.BLACK; isAntiAlias = false }
                for (r in modules.indices) for (c in modules.indices) {
                    if (modules[r][c]) canvas.drawRect(margin + c * cell, y + r * cell, margin + (c + 1) * cell, y + (r + 1) * cell, dark)
                }
                draw(context.getString(R.string.pdf_scan_to_pay, "₹" + money(due)), margin + size + 10f, y + size / 2 - 6f, 250f, 10f, bold = true)
                y += size + 10f
            }

            ensure(50f)
            val sigX = margin + contentW - 170f
            val p = Paint().apply { color = Color.GRAY; strokeWidth = 0.7f }
            canvas.drawLine(sigX, y + 34f, sigX + 170f, y + 34f, p)
            draw(context.getString(R.string.pdf_signatory), sigX, y + 37f, 170f, 8f, align = Layout.Alignment.ALIGN_CENTER)
            y += 50f
        }
    }
}
