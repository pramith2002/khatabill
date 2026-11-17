package com.teczo.khatabill.ui.reports

import com.teczo.khatabill.domain.report.DateRange
import com.teczo.khatabill.domain.report.DateRanges
import com.teczo.khatabill.domain.report.RangeKind
import com.teczo.khatabill.util.dates.DateFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/** The date filter state shared by the report and book view models. Starts on "this month". */
class RangeSelector(private val fyStartMonth: Int = 4) {
    val kind = MutableStateFlow(RangeKind.THIS_MONTH)
    val custom = MutableStateFlow(DateRanges.of(RangeKind.THIS_MONTH, DateFormat.today(), fyStartMonth))

    val range: Flow<DateRange> = combine(kind, custom) { k, c ->
        if (k == RangeKind.CUSTOM) c else DateRanges.of(k, DateFormat.today(), fyStartMonth)
    }

    fun select(k: RangeKind) {
        if (k == RangeKind.CUSTOM) {
            // start the custom range from whatever was on screen
            custom.value = DateRanges.of(kind.value.takeIf { it != RangeKind.CUSTOM } ?: RangeKind.THIS_MONTH, DateFormat.today(), fyStartMonth)
        }
        kind.value = k
    }
}
