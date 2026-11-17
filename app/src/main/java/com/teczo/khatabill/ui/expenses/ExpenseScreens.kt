package com.teczo.khatabill.ui.expenses

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teczo.khatabill.R
import com.teczo.khatabill.domain.model.ExpenseCategories
import com.teczo.khatabill.domain.model.PaymentMode
import com.teczo.khatabill.ui.components.AccentButton
import com.teczo.khatabill.ui.components.AddFab
import com.teczo.khatabill.ui.components.AppTextField
import com.teczo.khatabill.ui.components.DateField
import com.teczo.khatabill.ui.components.EmptyState
import com.teczo.khatabill.ui.components.ScreenHeader
import com.teczo.khatabill.ui.components.TealCard
import com.teczo.khatabill.ui.util.Labels
import com.teczo.khatabill.util.dates.DateFormat
import com.teczo.khatabill.util.money.MoneyFormatter
import com.teczo.khatabill.util.money.MoneyInput
import java.io.File

@Composable
private fun categoryLabel(key: String): String = ExpenseCategories.labelRes(key)?.let { stringResource(it) } ?: key

@Composable
fun ExpenseListScreen(onBack: () -> Unit, onAdd: () -> Unit, viewModel: ExpenseListViewModel = hiltViewModel()) {
    val list by viewModel.items.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(stringResource(R.string.more_expenses), onBack)
            if (list.isEmpty()) {
                EmptyState(stringResource(R.string.expenses_empty), stringResource(R.string.expenses_hint))
            } else {
                TealCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(stringResource(R.string.total_grand), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.amount_rupees, MoneyFormatter.format(list.sumOf { it.amount })), style = MaterialTheme.typography.headlineMedium)
                }
                LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)) {
                    items(list, key = { it.id }) { e ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { pendingDelete = e.id }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(categoryLabel(e.category), style = MaterialTheme.typography.titleMedium)
                                Text(DateFormat.format(e.date) + " · " + stringResource(Labels.paymentMode(e.mode)), style = MaterialTheme.typography.bodySmall)
                                e.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                if (e.receiptUri != null) Text(stringResource(R.string.expense_receipt_attached), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                            }
                            Text(stringResource(R.string.amount_rupees, MoneyFormatter.format(e.amount)), style = MaterialTheme.typography.titleMedium)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
        AddFab(stringResource(R.string.expense_add), onAdd, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }

    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.expense_delete_title)) },
            text = { Text(stringResource(R.string.expense_delete_message)) },
            confirmButton = { TextButton(onClick = { viewModel.delete(id); pendingDelete = null }) { Text(stringResource(R.string.btn_delete)) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.btn_cancel)) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseEditScreen(onDone: () -> Unit, viewModel: ExpenseEditViewModel = hiltViewModel()) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pending by remember { mutableStateOf<File?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        pending?.let { f -> if (ok) viewModel.update { it.copy(receiptPath = f.absolutePath) } else f.delete() }
        pending = null
    }

    LaunchedEffect(done) { if (done) onDone() }

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        ScreenHeader(stringResource(R.string.expense_title_new), onDone)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.field_category), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExpenseCategories.keys.forEach { key ->
                    FilterChip(
                        selected = form.category == key,
                        onClick = { viewModel.update { it.copy(category = key) } },
                        label = { Text(categoryLabel(key), style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
            AppTextField(
                form.amount, { v -> viewModel.update { it.copy(amount = v) } }, stringResource(R.string.field_amount), keyboardType = KeyboardType.Decimal,
                error = when {
                    error != null -> R.string.err_amount
                    form.amount.isNotBlank() && MoneyInput.parsePaise(form.amount) == null -> R.string.err_number
                    else -> null
                },
            )
            Text(stringResource(R.string.field_payment_mode), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentMode.entries.forEach { m ->
                    FilterChip(
                        selected = form.mode == m, onClick = { viewModel.update { it.copy(mode = m) } },
                        label = { Text(stringResource(Labels.paymentMode(m)), style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
            DateField(stringResource(R.string.field_date), form.date, { d -> d?.let { v -> viewModel.update { it.copy(date = v) } } })
            AppTextField(form.notes, { v -> viewModel.update { it.copy(notes = v) } }, stringResource(R.string.field_notes), singleLine = false)

            OutlinedButton(
                onClick = {
                    val file = viewModel.newReceiptFile()
                    pending = file
                    camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text(stringResource(R.string.expense_receipt_add)) }
            form.receiptPath?.let { path ->
                val bitmap = remember(path) { BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 8 }) }
                if (bitmap != null) {
                    Image(bitmap.asImageBitmap(), contentDescription = stringResource(R.string.expense_receipt_attached), modifier = Modifier.size(120.dp))
                }
                Text(stringResource(R.string.expense_receipt_attached), style = MaterialTheme.typography.bodySmall)
            }
        }
        AccentButton(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text(stringResource(R.string.btn_save)) }
    }
}
