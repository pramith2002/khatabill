package com.teczo.khatabill.ui.reports

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teczo.khatabill.R
import com.teczo.khatabill.domain.report.Cell
import com.teczo.khatabill.domain.report.ReportTable
import com.teczo.khatabill.ui.components.WhiteCard

@Composable
private fun cellString(c: Cell): String = c.res?.let { stringResource(it) } ?: c.text

/** A report table that scrolls sideways when it has many columns (GST tables have up to nine). */
@Composable
fun ReportTableView(table: ReportTable, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        table.title?.let { Text(stringResource(it), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp)) }
        if (table.isEmpty) {
            Text(stringResource(R.string.report_empty), style = MaterialTheme.typography.bodyMedium)
            return
        }
        WhiteCard(Modifier.fillMaxWidth()) {
            Column(Modifier.horizontalScroll(rememberScrollState())) {
                TableRow(table, table.columns.map { Cell(stringResource(it.label)) }, bold = true)
                HorizontalDivider()
                table.rows.forEach { row ->
                    TableRow(table, row, bold = false)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                table.total?.let { TableRow(table, it, bold = true) }
            }
        }
    }
}

@Composable
private fun TableRow(table: ReportTable, cells: List<Cell>, bold: Boolean) {
    Row(Modifier.padding(vertical = 6.dp)) {
        cells.forEachIndexed { i, cell ->
            val numeric = table.columns[i].right
            Text(
                cellString(cell),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                textAlign = if (numeric) TextAlign.End else TextAlign.Start,
                modifier = Modifier.width(if (numeric) 104.dp else 150.dp).padding(horizontal = 6.dp),
            )
        }
    }
}
