package com.teczo.khatabill.util.pdf

import android.content.Context
import android.graphics.Color
import android.text.Layout
import com.teczo.khatabill.R
import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.domain.report.Cell
import com.teczo.khatabill.domain.report.ReportTable
import java.io.File

/** Any report (one or several tables) as an A4 PDF for the accountant. Long tables continue on the next page. */
class ReportPdfBuilder(private val context: Context) {

    private fun cellText(c: Cell) = c.res?.let { context.getString(it) } ?: c.text

    fun build(business: BusinessEntity, titleRes: Int, rangeText: String?, tables: List<ReportTable>, noteRes: Int?, file: File): File {
        val w = PdfWriter(context, footerLeft = business.name, pageLabel = { context.getString(R.string.pdf_page, it) })
        w.newPage()
        w.y += w.text(business.name, w.margin, w.y, w.contentW, 15f, bold = true) + 2
        business.gstin?.let { w.y += w.text(context.getString(R.string.pdf_gstin, it), w.margin, w.y, w.contentW, 9f) + 2 }
        w.y += w.text(context.getString(titleRes), w.margin, w.y + 4, w.contentW, 13f, bold = true) + 6
        rangeText?.let { w.y += w.text(it, w.margin, w.y, w.contentW, 9f, color = Color.DKGRAY) + 2 }
        w.y += 6
        w.rule(); w.y += 8

        for (table in tables) {
            table.title?.let {
                w.ensure(40f)
                w.y += w.text(context.getString(it), w.margin, w.y, w.contentW, 11f, bold = true) + 4
            }
            if (table.isEmpty) {
                w.y += w.text(context.getString(R.string.report_empty), w.margin, w.y, w.contentW, 9f) + 10
                continue
            }
            drawTable(w, table)
            w.y += 14
        }
        noteRes?.let {
            w.ensure(30f)
            w.y += w.text(context.getString(it), w.margin, w.y, w.contentW, 8f, color = Color.DKGRAY)
        }
        return w.write(file)
    }

    private fun drawTable(w: PdfWriter, t: ReportTable) {
        val n = t.columns.size
        val size = if (n > 7) 7.5f else 9f
        // numeric columns get a fixed share; text columns split the rest
        val numeric = t.columns.count { it.right }
        val numW = if (n > 7) 58f else 74f
        val textW = ((w.contentW - numeric * numW) / (n - numeric).coerceAtLeast(1)).coerceAtLeast(40f)
        val widths = t.columns.map { if (it.right) numW else textW }
        val scale = (w.contentW / widths.sum()).coerceAtMost(1f)
        val ws = widths.map { it * scale }
        fun x(i: Int) = w.margin + ws.take(i).sum()

        fun header() {
            w.ensure(36f)
            w.fillRect(w.margin, w.y, w.margin + w.contentW, w.y + 18f, Color.rgb(238, 238, 238))
            t.columns.forEachIndexed { i, c ->
                w.text(context.getString(c.label), x(i) + 2f, w.y + 4f, ws[i] - 4f, 8f, bold = true, align = if (c.right) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL)
            }
            w.y += 20f
        }

        fun row(cells: List<Cell>, bold: Boolean) {
            val texts = cells.map(::cellText)
            val h = (texts.mapIndexed { i, s -> w.height(s, ws[i] - 4f, size, bold) }.maxOrNull() ?: 10f).coerceAtLeast(10f) + 6f
            w.ensure(h, onNewPage = ::header)
            texts.forEachIndexed { i, s ->
                w.text(s, x(i) + 2f, w.y + 3f, ws[i] - 4f, size, bold = bold, align = if (t.columns[i].right) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL)
            }
            w.y += h
            w.canvas.drawLine(w.margin, w.y - 1, w.margin + w.contentW, w.y - 1, android.graphics.Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f })
        }

        header()
        t.rows.forEach { row(it, bold = false) }
        t.total?.let { row(it, bold = true) }
    }
}
