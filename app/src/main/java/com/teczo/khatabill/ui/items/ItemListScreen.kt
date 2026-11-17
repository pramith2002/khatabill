package com.teczo.khatabill.ui.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teczo.khatabill.R
import com.teczo.khatabill.ui.components.AddFab
import com.teczo.khatabill.ui.components.AppTextField
import com.teczo.khatabill.ui.components.EmptyState
import com.teczo.khatabill.ui.util.unitLabel
import com.teczo.khatabill.util.money.MoneyFormatter
import com.teczo.khatabill.util.money.MoneyInput

@Composable
fun ItemListScreen(onAdd: () -> Unit, onOpen: (String) -> Unit, viewModel: ItemListViewModel = hiltViewModel()) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            Text(stringResource(R.string.nav_items), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
            AppTextField(
                value = query,
                onValueChange = { viewModel.query.value = it },
                label = stringResource(R.string.search_hint),
                supportingText = stringResource(R.string.items_hint),
                capitalization = KeyboardCapitalization.None,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            if (rows.isEmpty() && query.isBlank()) {
                EmptyState(stringResource(R.string.items_empty), stringResource(R.string.items_empty_hint))
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(rows, key = { it.item.id }) { row ->
                        val item = row.item
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { onOpen(item.id) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                item.nameKn?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                Text(
                                    stringResource(R.string.amount_rupees, MoneyFormatter.format(item.salePrice)),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (item.type == com.teczo.khatabill.domain.model.ItemType.PRODUCT) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        stringResource(R.string.qty_unit, MoneyInput.qtyToText(row.stock), unitLabel(item.unit)),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (row.isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (row.isLow) {
                                        Text(stringResource(R.string.item_low_stock), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
        AddFab(stringResource(R.string.item_add), onAdd, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
}
