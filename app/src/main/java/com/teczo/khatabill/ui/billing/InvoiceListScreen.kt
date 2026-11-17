package com.teczo.khatabill.ui.billing

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.ui.components.AddFab
import com.teczo.khatabill.ui.components.EmptyState
import com.teczo.khatabill.ui.util.Labels
import com.teczo.khatabill.util.dates.DateFormat
import com.teczo.khatabill.util.money.MoneyFormatter

@Composable
fun InvoiceListScreen(
    onNew: (DocumentType) -> Unit,
    onOpen: (String) -> Unit,
    viewModel: InvoiceListViewModel = hiltViewModel(),
) {
    val list by viewModel.items.collectAsStateWithLifecycle()
    val type by viewModel.type.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            Text(stringResource(R.string.nav_billing), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                billingTabs.forEach { t ->
                    FilterChip(
                        selected = t == type,
                        onClick = { viewModel.type.value = t },
                        label = { Text(stringResource(Labels.docType(t)), style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
            if (list.isEmpty()) {
                EmptyState(stringResource(R.string.invoices_empty), stringResource(R.string.invoices_empty_hint))
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(list, key = { it.id }) { inv ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { onOpen(inv.id) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(inv.number, style = MaterialTheme.typography.titleMedium)
                                Text(inv.partyName ?: stringResource(R.string.walk_in_customer), style = MaterialTheme.typography.bodyMedium)
                                Text(DateFormat.format(inv.date), style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(stringResource(R.string.amount_rupees, MoneyFormatter.format(inv.grandTotal)), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stringResource(Labels.status(inv.status)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (inv.status == InvoiceStatus.CANCELLED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
        AddFab(stringResource(R.string.invoice_new), { onNew(type) }, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
}
