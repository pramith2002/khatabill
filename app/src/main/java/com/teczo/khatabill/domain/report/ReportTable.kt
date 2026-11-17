package com.teczo.khatabill.domain.report

import androidx.annotation.StringRes
import com.teczo.khatabill.util.money.MoneyFormatter
import com.teczo.khatabill.util.money.MoneyInput
import com.teczo.khatabill.util.dates.DateFormat

/**
 * One cell of a report. [text] is what the screen and PDF show (Indian grouping, 1,23,456.00);
 * [raw] is what goes into the CSV (plain 123456.00, so Excel reads it as a number). When [res] is set,
 * the cell is a translated label instead of data.
 */
data class Cell(val text: String, val raw: String = text, val right: Boolean = false, @StringRes val res: Int? = null)

data class Col(@StringRes val label: Int, val right: Boolean = false)

data class ReportTable(
    @StringRes val title: Int?,
    val columns: List<Col>,
    val rows: List<List<Cell>>,
    val total: List<Cell>? = null,
) {
    val isEmpty: Boolean get() = rows.isEmpty()
}

object Cells {
    fun text(s: String?) = Cell(s.orEmpty())

    fun label(@StringRes res: Int) = Cell("", "", false, res)

    fun money(paise: Long) = Cell(MoneyFormatter.format(paise), plain(paise), right = true)

    fun qty(thousandths: Long) = Cell(MoneyInput.qtyToText(thousandths), MoneyInput.qtyToText(thousandths), right = true)

    fun count(n: Int) = Cell(n.toString(), n.toString(), right = true)

    fun date(epochDay: Long) = Cell(DateFormat.format(epochDay))

    fun percent(basisPoints: Int) = Cell(MoneyInput.bpToText(basisPoints) + "%", MoneyInput.bpToText(basisPoints), right = true)

    /** 123456.00, with a leading minus for negatives. No grouping, so spreadsheets read it as a number. */
    fun plain(paise: Long): String {
        val negative = paise < 0
        val digits = paise.toString().removePrefix("-").padStart(3, '0')
        return (if (negative) "-" else "") + digits.dropLast(2) + "." + digits.takeLast(2)
    }
}
