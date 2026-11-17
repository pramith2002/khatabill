package com.teczo.khatabill.ui.credit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.teczo.khatabill.domain.ledger.AgeBucket
import com.teczo.khatabill.ui.components.EmptyState
import com.teczo.khatabill.ui.components.ScreenHeader
import com.teczo.khatabill.ui.components.TealCard
import com.teczo.khatabill.ui.components.WhiteCard
import com.teczo.khatabill.util.money.MoneyFormatter

private fun AgeBucket.label(): Int = when (this) {
    AgeBucket.DAYS_0_30 -> R.string.aging_0_30
    AgeBucket.DAYS_31_60 -> R.string.aging_31_60
    AgeBucket.DAYS_61_90 -> R.string.aging_61_90
    AgeBucket.DAYS_90_PLUS -> R.string.aging_90_plus
}

@Composable
private fun rupees(paise: Long) = stringResource(R.string.amount_rupees, MoneyFormatter.format(paise))

/** Aging report: who owes what, split by how old the bills are, with a one-tap reminder. */
@Composable
fun CollectionsScreen(
    onBack: () -> Unit,
    onOpenParty: (String) -> Unit,
    onRemind: (String) -> Unit,
    viewModel: CollectionsViewModel = hiltViewModel(),
) {
    val report by viewModel.report.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        ScreenHeader(stringResource(R.string.more_collections), onBack)
        if (report.rows.isEmpty()) {
            EmptyState(stringResource(R.string.collections_empty), stringResource(R.string.aging_hint))
            return@Column
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TealCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_to_collect), style = MaterialTheme.typography.titleMedium)
                Text(rupees(report.grandTotal), style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(R.string.aging_hint), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                AgeBucket.entries.forEach { b ->
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(b.label()), style = MaterialTheme.typography.bodyLarge)
                        Text(rupees(report.totals.getValue(b)), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            report.rows.forEach { row ->
                WhiteCard(Modifier.fillMaxWidth().clickable { onOpenParty(row.party.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(row.party.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(rupees(row.aging.total), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    AgeBucket.entries.filter { row.aging.buckets.getValue(it) > 0 }.forEach { b ->
                        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(b.label()), style = MaterialTheme.typography.bodyMedium)
                            Text(rupees(row.aging.buckets.getValue(b)), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    OutlinedButton(onClick = { onRemind(row.party.id) }, modifier = Modifier.padding(top = 8.dp).fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.party_remind))
                    }
                }
            }
        }
    }
}
