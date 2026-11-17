package com.teczo.khatabill.ui.books

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teczo.khatabill.R
import com.teczo.khatabill.domain.model.ExpenseCategories
import com.teczo.khatabill.domain.report.DayBookEntry
import com.teczo.khatabill.domain.report.MoneyMovement
import com.teczo.khatabill.ui.components.DateField
import com.teczo.khatabill.ui.components.EmptyState
import com.teczo.khatabill.ui.components.RangePicker
import com.teczo.khatabill.ui.components.ScreenHeader
import com.teczo.khatabill.ui.components.TealCard
import com.teczo.khatabill.ui.util.Labels
import com.teczo.khatabill.util.dates.DateFormat
import com.teczo.khatabill.util.money.MoneyFormatter

@Composable
private fun rupees(paise: Long) = stringResource(R.string.amount_rupees, MoneyFormatter.format(paise))

/** Cash book or bank book: money in, money out, and the balance after each entry. */
@Composable
fun MoneyBookScreen(onBack: () -> Unit, viewModel: BookViewModel = hiltViewModel()) {
    val result by viewModel.result.collectAsStateWithLifecycle()
    val kind by viewModel.selector.kind.collectAsStateWithLifecycle()
    val custom by viewModel.selector.custom.collectAsStateWithLifecycle()
    val range by viewModel.selector.range.collectAsStateWithLifecycle(initialValue = custom)
    val title = if (viewModel.book == "bank") R.string.more_bank_book else R.string.more_cash_book

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        ScreenHeader(stringResource(title), onBack)
        RangePicker(kind, range, viewModel.selector::select, { viewModel.selector.custom.value = it }, Modifier.padding(horizontal = 16.dp))
        val r = result ?: return@Column
        TealCard(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.statement_opening), style = MaterialTheme.typography.bodyLarge)
                Text(rupees(r.opening), style = MaterialTheme.typography.titleMedium)
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.book_in), style = MaterialTheme.typography.bodyLarge)
                Text("+ " + rupees(r.totalIn), style = MaterialTheme.typography.titleMedium)
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.book_out), style = MaterialTheme.typography.bodyLarge)
                Text("- " + rupees(r.totalOut), style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.4f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.statement_closing), style = MaterialTheme.typography.titleMedium)
                Text(rupees(r.closing), style = MaterialTheme.typography.headlineSmall)
            }
        }
        if (r.rows.isEmpty()) {
            EmptyState(stringResource(R.string.report_empty), "")
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(r.rows) { row ->
                    val m = row.movement
                    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(movementTitle(m), style = MaterialTheme.typography.titleMedium)
                            Text(DateFormat.format(m.date) + " · " + stringResource(Labels.paymentMode(m.mode)), style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (m.amountIn > 0) Text("+ " + rupees(m.amountIn), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            if (m.amountOut > 0) Text("- " + rupees(m.amountOut), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
                            Text(rupees(row.balanceAfter), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun movementTitle(m: MoneyMovement): String = when (m.kind) {
    MoneyMovement.Kind.PAYMENT_IN -> listOfNotNull(m.partyName, stringResource(R.string.payment_received)).joinToString(" · ")
    MoneyMovement.Kind.PAYMENT_OUT -> listOfNotNull(m.partyName, stringResource(R.string.payment_paid)).joinToString(" · ")
    MoneyMovement.Kind.EXPENSE -> stringResource(R.string.daybook_expense) + ": " + (m.label?.let { ExpenseCategories.labelRes(it)?.let { r -> stringResource(r) } ?: it }.orEmpty())
}

@Composable
private fun entryLabel(e: DayBookEntry): String = stringResource(
    when (e.kind) {
        DayBookEntry.Kind.SALE -> R.string.doc_sale
        DayBookEntry.Kind.PURCHASE -> R.string.doc_purchase
        DayBookEntry.Kind.SALE_RETURN -> R.string.daybook_sale_return
        DayBookEntry.Kind.PURCHASE_RETURN -> R.string.daybook_purchase_return
        DayBookEntry.Kind.PAYMENT_IN -> R.string.payment_received
        DayBookEntry.Kind.PAYMENT_OUT -> R.string.payment_paid
        DayBookEntry.Kind.EXPENSE -> R.string.daybook_expense
    },
)

/** Day book: every bill, payment and expense of one day. */
@Composable
fun DayBookScreen(onBack: () -> Unit, viewModel: BookViewModel = hiltViewModel()) {
    val entries by viewModel.dayEntries.collectAsStateWithLifecycle()
    val day by viewModel.day.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        ScreenHeader(stringResource(R.string.more_day_book), onBack)
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.day.value = day - 1 }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back)) }
            DateField(stringResource(R.string.field_date), day, { d -> d?.let { viewModel.day.value = it } }, Modifier.weight(1f))
            IconButton(onClick = { viewModel.day.value = day + 1 }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.btn_next)) }
        }
        if (entries.isEmpty()) {
            EmptyState(stringResource(R.string.daybook_empty), "")
        } else {
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(entries) { e ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entryLabel(e) + (if (e.title.isNotBlank()) " · " + e.title else ""), style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOfNotNull(e.partyName, e.mode?.let { stringResource(Labels.paymentMode(it)) }).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(rupees(e.amount), style = MaterialTheme.typography.titleMedium)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
