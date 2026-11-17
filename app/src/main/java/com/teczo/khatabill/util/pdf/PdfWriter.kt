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
import com.teczo.khatabill.util.qr.QrCode
import java.io.File

/**
 * Small A4 page writer used by the statement and report PDFs. It owns the page flow (new page when the
 * current one is full) and draws text with the bundled Noto font for each script, so Kannada and Devanagari
 * conjuncts shape correctly. The invoice PDF has its own renderer; this one is the reusable version.
 */
class PdfWriter(private val context: Context, private val footerLeft: String, private val pageLabel: (Int) -> String) {
    val pageW = 595
    val pageH = 842
    val margin = 32f
    val contentW = pageW - 2 * margin
    private val bottomLimit = pageH - 56f

    private val doc = PdfDocument()
    private var page: PdfDocument.Page? = null
    lateinit var canvas: Canvas
        private set
    var y = margin
    var pageNo = 0
        private set

    private val kannada: Typeface? by lazy { ResourcesCompat.getFont(context, R.font.noto_sans_kannada) }
    private val devanagari: Typeface? by lazy { ResourcesCompat.getFont(context, R.font.noto_sans_devanagari) }

    private fun faceFor(text: String): Typeface? {
        for (ch in text) {
            when (ch.code) {
                in 0x0C80..0x0CFF -> return kannada
                in 0x0900..0x097F -> return devanagari
            }
        }
        return null
    }

    private fun layout(text: String, width: Float, size: Float, bold: Boolean, align: Layout.Alignment, color: Int): StaticLayout {
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            isFakeBoldText = bold
            this.color = color
            faceFor(text)?.let { typeface = it }
        }
        return StaticLayout.Builder.obtain(text, 0, text.length, p, width.toInt().coerceAtLeast(1))
            .setAlignment(align).setLineSpacing(0f, 1.15f).setIncludePad(false).build()
    }

    fun newPage() {
        page?.let { finishPage(it) }
        pageNo += 1
        val p = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
        page = p
        canvas = p.canvas
        y = margin
    }

    private fun finishPage(p: PdfDocument.Page) {
        text(footerLeft, margin, pageH - 40f, contentW / 2, 8f, color = Color.DKGRAY)
        text(pageLabel(pageNo), margin + contentW / 2, pageH - 40f, contentW / 2, 8f, align = Layout.Alignment.ALIGN_OPPOSITE, color = Color.DKGRAY)
        doc.finishPage(p)
    }

    /** Draws text with its top-left at ([x], [top]) and returns the height used. Does not move [y]. */
    fun text(
        text: String, x: Float, top: Float, width: Float, size: Float, bold: Boolean = false,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL, color: Int = Color.BLACK,
    ): Float {
        if (text.isEmpty()) return 0f
        val l = layout(text, width, size, bold, align, color)
        canvas.save()
        canvas.translate(x, top)
        l.draw(canvas)
        canvas.restore()
        return l.height.toFloat()
    }

    fun height(text: String, width: Float, size: Float, bold: Boolean = false): Float =
        if (text.isEmpty()) 0f else layout(text, width, size, bold, Layout.Alignment.ALIGN_NORMAL, Color.BLACK).height.toFloat()

    /** Starts a new page when [h] points will not fit; [onNewPage] redraws a table header. */
    fun ensure(h: Float, onNewPage: (() -> Unit)? = null) {
        if (y + h <= bottomLimit) return
        newPage()
        onNewPage?.invoke()
    }

    fun rule(color: Int = Color.GRAY) {
        canvas.drawLine(margin, y, margin + contentW, y, Paint().apply { this.color = color; strokeWidth = 0.7f })
    }

    fun fillRect(left: Float, top: Float, right: Float, bottom: Float, color: Int) {
        canvas.drawRect(left, top, right, bottom, Paint().apply { this.color = color })
    }

    /** Draws a QR code of [content] with its top-left at ([x], [top]), [size] points wide. Vector squares, so it scans at any zoom. */
    fun qr(content: String, x: Float, top: Float, size: Float) {
        val modules = QrCode.modules(content)
        val n = modules.size
        val cell = size / n
        val paint = Paint().apply { color = Color.BLACK; isAntiAlias = false }
        for (row in 0 until n) for (col in 0 until n) {
            if (modules[row][col]) canvas.drawRect(x + col * cell, top + row * cell, x + (col + 1) * cell, top + (row + 1) * cell, paint)
        }
    }

    fun write(file: File): File {
        page?.let { finishPage(it) }
        page = null
        file.parentFile?.mkdirs()
        try {
            file.outputStream().use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
        return file
    }
}
