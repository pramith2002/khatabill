package com.teczo.khatabill.util.dates

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FinancialYearTest {
    private fun label(y: Int, m: Int, d: Int, start: Int = 4) =
        FinancialYear.label(LocalDate.of(y, m, d).toEpochDay(), start)

    @Test fun firstDayOfYear() = assertEquals("2026-27", label(2026, 4, 1))
    @Test fun lastDayOfYear() = assertEquals("2026-27", label(2027, 3, 31))
    @Test fun dayBeforeNewYear() = assertEquals("2025-26", label(2026, 3, 31))
    @Test fun midYear() = assertEquals("2026-27", label(2026, 11, 17))
    @Test fun centuryWrap() = assertEquals("2099-00", label(2099, 5, 1))
    @Test fun januaryStartIsCalendarYear() = assertEquals("2026", label(2026, 7, 1, start = 1))
    @Test fun shortLabel() = assertEquals("26-27", FinancialYear.shortLabel("2026-27"))
}
