package com.teczo.khatabill.ui.payments

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teczo.khatabill.R
import com.teczo.khatabill.domain.model.PaymentType
import com.teczo.khatabill.ui.components.AddFab
import com.teczo.khatabill.ui.components.EmptyState
import com.teczo.khatabill.ui.components.PeachCard
import com.teczo.khatabill.ui.components.ScreenHeader
import com.teczo.khatabill.ui.util.Labels
import com.teczo.khatabill.util.dates.DateFormat
import com.teczo.khatabill.util.money.MoneyFormatter

@Composable
fun PaymentListScreen(onBack: () -> Unit, onAdd: () -> Unit, viewModel: PaymentListViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(stringResource(R.string.more_payments), onBack)
            if (state.rows.isEmpty()) {
                EmptyState(stringResource(R.string.payments_empty), stringResource(R.string.payments_empty_hint))
            } else {
                if (state.receivedByMode.isNotEmpty()) {
                    PeachCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Text(stringResource(R.string.payments_by_mode), style = MaterialTheme.typography.titleMedium)
                        state.receivedByMode.entries.sortedByDescending { it.value }.forEach { (mode, total) ->
                            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(Labels.paymentMode(mode)), style = MaterialTheme.typography.bodyLarge)
                                Text(stringResource(R.string.amount_rupees, MoneyFormatter.format(total)), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)) {
                    items(state.rows, key = { it.payment.id }) { row ->
                        val p = row.payment
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { pendingDelete = p.id }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(row.partyName ?: stringResource(R.string.payment_walk_in), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stringResource(Labels.paymentMode(p.mode)) + " · " + DateFormat.format(p.date),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            val received = p.type == PaymentType.RECEIVED
                            Text(
                                (if (received) "+ " else "- ") + stringResource(R.string.amount_rupees, MoneyFormatter.format(p.amount)),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (received) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
        AddFab(stringResource(R.string.qa_add_payment), onAdd, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }

    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.payment_delete_title)) },
            text = { Text(stringResource(R.string.payment_delete_message)) },
            confirmButton = { TextButton(onClick = { viewModel.delete(id); pendingDelete = null }) { Text(stringResource(R.string.btn_delete)) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.btn_cancel)) } },
        )
    }
}
