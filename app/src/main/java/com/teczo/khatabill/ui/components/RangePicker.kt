package com.teczo.khatabill.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.teczo.khatabill.R
import com.teczo.khatabill.domain.report.DateRange
import com.teczo.khatabill.domain.report.RangeKind

private fun RangeKind.label(): Int = when (this) {
    RangeKind.TODAY -> R.string.range_today
    RangeKind.THIS_WEEK -> R.string.range_week
    RangeKind.THIS_MONTH -> R.string.range_month
    RangeKind.THIS_QUARTER -> R.string.range_quarter
    RangeKind.THIS_FY -> R.string.range_fy
    RangeKind.CUSTOM -> R.string.range_custom
}

/** Date filter used by reports and books: today, week, month, quarter, financial year, or custom dates. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RangePicker(
    kind: RangeKind,
    range: DateRange,
    onKind: (RangeKind) -> Unit,
    onCustom: (DateRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RangeKind.entries.forEach { k ->
                FilterChip(
                    selected = kind == k,
                    onClick = { onKind(k) },
                    label = { Text(stringResource(k.label()), style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        if (kind == RangeKind.CUSTOM) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(stringResource(R.string.range_from), range.from, { d -> d?.let { onCustom(DateRange(minOf(it, range.to), range.to)) } }, Modifier.weight(1f))
                DateField(stringResource(R.string.range_to), range.to, { d -> d?.let { onCustom(DateRange(range.from, maxOf(it, range.from))) } }, Modifier.weight(1f))
            }
        }
    }
}
