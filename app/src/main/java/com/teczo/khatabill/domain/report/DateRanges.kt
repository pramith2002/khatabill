package com.teczo.khatabill.domain.report

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** An inclusive range of epoch days. */
data class DateRange(val from: Long, val to: Long) {
    init {
        require(from <= to) { "Range start must not be after its end" }
    }
    operator fun contains(day: Long) = day in from..to
}

enum class RangeKind { TODAY, THIS_WEEK, THIS_MONTH, THIS_QUARTER, THIS_FY, CUSTOM }

object DateRanges {
    /**
     * The range for a quick filter. Weeks run Monday to Sunday. Quarters follow the financial year
     * (April to June, July to September, ...). The financial year starts in [fyStartMonth] (April).
     */
    fun of(kind: RangeKind, today: Long, fyStartMonth: Int = 4): DateRange {
        val d = LocalDate.ofEpochDay(today)
        return when (kind) {
            RangeKind.TODAY, RangeKind.CUSTOM -> DateRange(today, today)
            RangeKind.THIS_WEEK -> DateRange(
                d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay(),
                d.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).toEpochDay(),
            )
            RangeKind.THIS_MONTH -> DateRange(d.withDayOfMonth(1).toEpochDay(), d.withDayOfMonth(d.lengthOfMonth()).toEpochDay())
            RangeKind.THIS_QUARTER -> {
                val fyStart = financialYearStart(d, fyStartMonth)
                val monthsIn = (d.year - fyStart.year) * 12 + (d.monthValue - fyStart.monthValue)
                val start = fyStart.plusMonths((monthsIn / 3 * 3).toLong())
                DateRange(start.toEpochDay(), start.plusMonths(3).minusDays(1).toEpochDay())
            }
            RangeKind.THIS_FY -> {
                val start = financialYearStart(d, fyStartMonth)
                DateRange(start.toEpochDay(), start.plusYears(1).minusDays(1).toEpochDay())
            }
        }
    }

    private fun financialYearStart(d: LocalDate, fyStartMonth: Int): LocalDate {
        val year = if (d.monthValue >= fyStartMonth) d.year else d.year - 1
        return LocalDate.of(year, fyStartMonth, 1)
    }
}
