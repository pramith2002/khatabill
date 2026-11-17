package com.teczo.khatabill.util.dates

import java.time.LocalDate

/** Indian financial year helpers. The year starts in [startMonth] (April by default). */
object FinancialYear {

    /** "2026-27" for dates from 1 April 2026 to 31 March 2027. A January start gives just "2026". */
    fun label(epochDay: Long, startMonth: Int = 4): String {
        require(startMonth in 1..12) { "startMonth must be 1..12" }
        val date = LocalDate.ofEpochDay(epochDay)
        if (startMonth == 1) return date.year.toString()
        val startYear = if (date.monthValue >= startMonth) date.year else date.year - 1
        val endShort = ((startYear + 1) % 100).toString().padStart(2, '0')
        return "$startYear-$endShort"
    }

    /** "26-27" from "2026-27" (used inside invoice numbers). */
    fun shortLabel(label: String): String = label.substring(2)
}
