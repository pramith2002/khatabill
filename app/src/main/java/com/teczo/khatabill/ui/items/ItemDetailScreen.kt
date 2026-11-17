package com.teczo.khatabill.ui.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teczo.khatabill.R
import com.teczo.khatabill.ui.components.AccentButton
import com.teczo.khatabill.domain.model.ItemType
import com.teczo.khatabill.domain.model.StockReason
import com.teczo.khatabill.ui.components.AppTextField
import com.teczo.khatabill.ui.components.RadioRow
import com.teczo.khatabill.ui.components.ScreenHeader
import com.teczo.khatabill.ui.util.unitLabel
import com.teczo.khatabill.util.money.MoneyFormatter
import com.teczo.khatabill.util.money.MoneyInput
import java.time.LocalDate

private fun StockReason.label(): Int = when (this) {
    StockReason.SALE -> R.string.reason_sale
    StockReason.PURCHASE -> R.string.reason_purchase
    StockReason.RETURN -> R.string.reason_return
    StockReason.ADJUSTMENT -> R.string.reason_adjustment
    StockReason.OPENING -> R.string.reason_opening
}

@Composable
fun ItemDetailScreen(onBack: () -> Unit, onEdit: (String) -> Unit, viewModel: ItemDetailViewModel = hiltViewModel()) {
    val item by viewModel.item.collectAsStateWithLifecycle()
    val stock by viewModel.stock.collectAsStateWithLifecycle()
    val movements by viewModel.movements.collectAsStateWithLifecycle()
    var showAdjust by remember { mutableStateOf(false) }

    val current = item
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        ScreenHeader(current?.name.orEmpty(), onBack)
        if (current == null) return@Column
        val isProduct = current.type == ItemType.PRODUCT
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            current.nameKn?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            Text(
                stringResource(R.string.amount_rupees, MoneyFormatter.format(current.salePrice)) + " / " + unitLabel(current.unit),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (isProduct) {
                val low = current.minStock > 0 && stock <= current.minStock
                Text(stringResource(R.string.item_stock), style = MaterialTheme.typography.labelLarge)
                Text(
                    stringResource(R.string.qty_unit, MoneyInput.qtyToText(stock), unitLabel(current.unit)),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                if (low) Text(stringResource(R.string.item_low_stock), color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { onEdit(current.id) }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.btn_edit))
                }
                if (isProduct) {
                    AccentButton(onClick = { showAdjust = true }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                        Text(stringResource(R.string.stock_adjust))
                    }
                }
            }
        }
        if (isProduct) {
            Text(stringResource(R.string.stock_history), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            LazyColumn {
                items(movements, key = { it.id }) { m ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(m.reason.label()), style = MaterialTheme.typography.bodyLarge)
                            Text(LocalDate.ofEpochDay(m.date).toString(), style = MaterialTheme.typography.bodySmall)
                            m.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                        val sign = if (m.qtyDelta > 0) "+" else ""
                        Text(sign + MoneyInput.qtyToText(m.qtyDelta), style = MaterialTheme.typography.titleMedium)
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAdjust) {
        var adding by remember { mutableStateOf(true) }
        var qty by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        val parsed = MoneyInput.parseQty(qty)
        AlertDialog(
            onDismissRequest = { showAdjust = false },
            title = { Text(stringResource(R.string.stock_adjust)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RadioRow(stringResource(R.string.stock_in), adding, { adding = true })
                    RadioRow(stringResource(R.string.stock_out), !adding, { adding = false })
                    AppTextField(qty, { qty = it }, stringResource(R.string.field_quantity), keyboardType = KeyboardType.Decimal, error = if (qty.isNotBlank() && parsed == null) R.string.err_number else null)
                    AppTextField(note, { note = it }, stringResource(R.string.field_note))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = parsed != null && parsed > 0,
                    onClick = { viewModel.adjust(if (adding) parsed!! else -parsed!!, note); showAdjust = false },
                ) { Text(stringResource(R.string.btn_save)) }
            },
            dismissButton = { TextButton(onClick = { showAdjust = false }) { Text(stringResource(R.string.btn_cancel)) } },
        )
    }
}
