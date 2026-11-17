package com.teczo.khatabill.util.pdf

import android.content.Context
import android.graphics.Color
import android.text.Layout
import com.teczo.khatabill.R
import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.data.entities.PartyEntity
import com.teczo.khatabill.domain.ledger.Statement
import com.teczo.khatabill.domain.ledger.StatementRow
import com.teczo.khatabill.domain.model.PaymentMode
import com.teczo.khatabill.util.dates.DateFormat
import com.teczo.khatabill.util.money.MoneyFormatter
import java.io.File
import java.time.LocalDate

/** A party statement of account: opening balance, every bill and payment with a running balance, closing balance. */
class StatementPdfBuilder(private val context: Context) {

    private fun modeText(mode: PaymentMode): String = context.getString(
        when (mode) {
            PaymentMode.CASH -> R.string.mode_cash
            PaymentMode.UPI -> R.string.mode_upi
            PaymentMode.CARD -> R.string.mode_card
            PaymentMode.BANK -> R.string.mode_bank
            PaymentMode.CHEQUE -> R.string.mode_cheque
        },
    )

    private fun money(paise: Long) = MoneyFormatter.format(paise)

    fun build(business: BusinessEntity, party: PartyEntity, statement: Statement): File {
        val w = PdfWriter(context, footerLeft = business.name, pageLabel = { context.getString(R.string.pdf_page, it) })
        val file = File(File(context.cacheDir, "invoices"), "statement-" + party.id.take(8) + ".pdf")

        // Column widths add up to contentW (531)
        val widths = floatArrayOf(70f, 231f, 75f, 75f, 80f)
        fun colX(i: Int) = w.margin + widths.take(i).sum()
        val headers = listOf(
            R.string.statement_col_date, R.string.statement_col_details, R.string.statement_col_bill,
            R.string.statement_col_paid, R.string.statement_col_balance,
        ).map { context.getString(it) }

        fun tableHeader() {
            w.ensure(40f)
            w.fillRect(w.margin, w.y, w.margin + w.contentW, w.y + 18f, Color.rgb(238, 238, 238))
            headers.forEachIndexed { i, text ->
                w.text(
                    text, colX(i) + 2f, w.y + 4f, widths[i] - 4f, 8f, bold = true,
                    align = if (i >= 2) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL,
                )
            }
            w.y += 20f
        }

        fun row(cells: List<String>, bold: Boolean = false) {
            val rowH = maxOf(w.height(cells[1], widths[1] - 4f, 9f, bold), 11f) + 6f
            w.ensure(rowH, onNewPage = ::tableHeader)
            cells.forEachIndexed { i, text ->
                w.text(
                    text, colX(i) + 2f, w.y + 3f, widths[i] - 4f, 9f, bold = bold,
                    align = if (i >= 2) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL,
                )
            }
            w.y += rowH
            w.canvas.drawLine(w.margin, w.y - 1, w.margin + w.contentW, w.y - 1, android.graphics.Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f })
        }

        w.newPage()
        // header: business on the left, title on the right
        val leftW = 330f
        var ly = w.y + w.text(business.name, w.margin, w.y, leftW, 16f, bold = true)
        business.gstin?.let { ly += w.text(context.getString(R.string.pdf_gstin, it), w.margin, ly + 2, leftW, 9f) + 2 }
        ly += w.text(context.getString(R.string.pdf_phone, business.phone), w.margin, ly + 2, leftW, 9f) + 2
        val ry = w.y + w.text(
            context.getString(R.string.statement_title), w.margin + 346f, w.y, w.contentW - 346f, 13f, bold = true,
            align = Layout.Alignment.ALIGN_OPPOSITE,
        )
        w.text(
            context.getString(R.string.pdf_date, DateFormat.format(LocalDate.now().toEpochDay())), w.margin + 346f, ry + 4, w.contentW - 346f, 9f,
            align = Layout.Alignment.ALIGN_OPPOSITE,
        )
        w.y = maxOf(ly, ry + 16f) + 8
        w.rule(); w.y += 8

        w.y += w.text(party.name, w.margin, w.y, w.contentW, 12f, bold = true) + 2
        party.nameKn?.takeIf { it.isNotBlank() }?.let { w.y += w.text(it, w.margin, w.y, w.contentW, 11f) + 2 }
        party.phone?.takeIf { it.isNotBlank() }?.let { w.y += w.text(it, w.margin, w.y, w.contentW, 9f) + 2 }
        party.gstin?.takeIf { it.isNotBlank() }?.let { w.y += w.text(context.getString(R.string.pdf_gstin, it), w.margin, w.y, w.contentW, 9f) + 2 }
        w.y += 8

        tableHeader()
        row(listOf("", context.getString(R.string.statement_opening), "", "", signed(statement.opening)), bold = true)
        for (r in statement.rows) {
            val details = if (r.kind == StatementRow.Kind.BILL) r.reference
            else context.getString(R.string.statement_payment_via, modeText(r.mode ?: PaymentMode.CASH))
            row(
                listOf(
                    DateFormat.format(r.date), details,
                    if (r.billAmount > 0) money(r.billAmount) else "",
                    if (r.paidAmount > 0) money(r.paidAmount) else "",
                    signed(r.balanceAfter),
                ),
            )
        }
        w.y += 6
        row(listOf("", context.getString(R.string.statement_closing), "", "", signed(statement.closing)), bold = true)
        w.y += 10
        w.ensure(30f)
        val closingText = when {
            statement.closing > 0 -> context.getString(R.string.statement_to_collect, "₹" + money(statement.closing))
            statement.closing < 0 -> context.getString(R.string.statement_to_pay, "₹" + money(-statement.closing))
            else -> context.getString(R.string.party_balance_settled)
        }
        w.text(closingText, w.margin, w.y, w.contentW, 12f, bold = true, align = Layout.Alignment.ALIGN_OPPOSITE)
        return w.write(file)
    }

    /** Positive = party owes us. Negative values show a minus sign (we owe them). */
    private fun signed(paise: Long) = if (paise < 0) "-" + money(-paise) else money(paise)
}
