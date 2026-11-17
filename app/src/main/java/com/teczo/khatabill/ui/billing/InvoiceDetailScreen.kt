package com.teczo.khatabill.ui.billing

import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teczo.khatabill.R
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.ui.components.AppTextField
import com.teczo.khatabill.ui.components.ScreenHeader
import com.teczo.khatabill.ui.util.Labels
import com.teczo.khatabill.ui.util.unitLabel
import com.teczo.khatabill.util.dates.DateFormat
import com.teczo.khatabill.util.money.MoneyFormatter
import com.teczo.khatabill.util.money.MoneyInput
import com.teczo.khatabill.util.pdf.PdfShare
import kotlinx.coroutines.launch

@Composable
private fun rupees(paise: Long) = stringResource(R.string.amount_rupees, MoneyFormatter.format(paise))

@Composable
private fun AmountRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge)
        Text(value, style = if (bold) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun InvoiceDetailScreen(onBack: () -> Unit, viewModel: InvoiceDetailViewModel = hiltViewModel()) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val business by viewModel.business.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCancel by remember { mutableStateOf(false) }

    val current = data
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        ScreenHeader(current?.invoice?.number.orEmpty(), onBack)
        if (current == null) return@Column
        val inv = current.invoice
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(Labels.docTitle(inv.type, inv.series)), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(Labels.status(inv.status)),
                style = MaterialTheme.typography.titleMedium,
                color = if (inv.status == InvoiceStatus.CANCELLED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            if (inv.status == InvoiceStatus.CANCELLED) {
                Text(stringResource(R.string.invoice_cancelled_reason, inv.cancelReason.orEmpty()), color = MaterialTheme.colorScheme.error)
            }
            Text(DateFormat.format(inv.date), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.invoice_bill_to) + ": " + (inv.partyName ?: stringResource(R.string.walk_in_customer)), style = MaterialTheme.typography.bodyLarge)

            current.lines.sortedBy { it.lineNo }.forEach { line ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(line.nameSnapshot, style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                MoneyInput.qtyToText(line.qty) + " " + unitLabel(line.unit) + " × " + rupees(line.rate),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(rupees(line.lineTotal), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    AmountRow(stringResource(R.string.total_subtotal), rupees(inv.subtotal))
                    if (inv.discountTotal > 0) AmountRow(stringResource(R.string.total_discount), "- " + rupees(inv.discountTotal))
                    AmountRow(stringResource(R.string.total_taxable), rupees(inv.taxableTotal))
                    if (inv.cgst > 0) AmountRow(stringResource(R.string.total_cgst), rupees(inv.cgst))
                    if (inv.sgst > 0) AmountRow(stringResource(R.string.total_sgst), rupees(inv.sgst))
                    if (inv.igst > 0) AmountRow(stringResource(R.string.total_igst), rupees(inv.igst))
                    if (inv.otherCharges > 0) AmountRow(stringResource(R.string.total_other), rupees(inv.otherCharges))
                    if (inv.roundOff != 0L) AmountRow(stringResource(R.string.total_roundoff), (if (inv.roundOff < 0) "- " else "+ ") + rupees(kotlin.math.abs(inv.roundOff)))
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    AmountRow(stringResource(R.string.total_grand), rupees(inv.grandTotal), bold = true)
                    if (inv.paidAmount > 0) {
                        AmountRow(stringResource(R.string.total_paid), rupees(inv.paidAmount))
                        AmountRow(stringResource(R.string.total_due), rupees(inv.grandTotal - inv.paidAmount))
                    }
                }
            }
        }
        val failed = stringResource(R.string.invoice_pdf_failed)
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (inv.status != InvoiceStatus.CANCELLED) {
                OutlinedButton(onClick = { showCancel = true }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.invoice_cancel))
                }
            }
            Button(
                onClick = {
                    val biz = business ?: return@Button
                    scope.launch {
                        try {
                            PdfShare.share(context, biz, current.invoice, current.lines.sortedBy { it.lineNo })
                        } catch (e: Exception) {
                            Toast.makeText(context, failed, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            ) { Text(stringResource(R.string.invoice_share)) }
        }
    }

    if (showCancel) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCancel = false },
            title = { Text(stringResource(R.string.invoice_cancel_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.invoice_cancel_hint))
                    AppTextField(reason, { reason = it }, stringResource(R.string.field_cancel_reason))
                }
            },
            confirmButton = {
                TextButton(enabled = reason.isNotBlank(), onClick = { viewModel.cancel(reason); showCancel = false }) {
                    Text(stringResource(R.string.invoice_cancel))
                }
            },
            dismissButton = { TextButton(onClick = { showCancel = false }) { Text(stringResource(R.string.btn_close)) } },
        )
    }
}
