package com.teczo.khatabill.ui.parties

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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teczo.khatabill.R
import com.teczo.khatabill.ui.components.AddFab
import com.teczo.khatabill.ui.components.AppTextField
import com.teczo.khatabill.ui.components.EmptyState
import com.teczo.khatabill.util.money.MoneyFormatter

@Composable
fun PartyListScreen(onAdd: () -> Unit, onOpen: (String) -> Unit, viewModel: PartyListViewModel = hiltViewModel()) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            Text(stringResource(R.string.nav_parties), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
            AppTextField(
                value = query,
                onValueChange = { viewModel.query.value = it },
                label = stringResource(R.string.search_hint),
                supportingText = stringResource(R.string.parties_hint),
                capitalization = KeyboardCapitalization.None,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    PartyFilter.ALL to R.string.filter_all,
                    PartyFilter.TO_COLLECT to R.string.filter_to_collect,
                    PartyFilter.TO_PAY to R.string.filter_to_pay,
                    PartyFilter.OVERDUE to R.string.filter_overdue,
                ).forEach { (f, label) ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { viewModel.filter.value = f },
                        label = { Text(stringResource(label), style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
            if (rows.isEmpty() && query.isBlank() && filter == PartyFilter.ALL) {
                EmptyState(stringResource(R.string.parties_empty), stringResource(R.string.parties_empty_hint))
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(rows, key = { it.party.id }) { row ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { onOpen(row.party.id) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(row.party.name, style = MaterialTheme.typography.titleMedium)
                                row.party.nameKn?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                row.party.phone?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            }
                            if (row.balance != 0L) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        stringResource(R.string.amount_rupees, MoneyFormatter.format(kotlin.math.abs(row.balance))),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (row.balance > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                    )
                                    Text(
                                        stringResource(if (row.balance > 0) R.string.party_balance_to_collect else R.string.party_balance_to_pay),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    if (row.overdue) {
                                        Text(stringResource(R.string.filter_overdue), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
        AddFab(stringResource(R.string.party_add), onAdd, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
}
