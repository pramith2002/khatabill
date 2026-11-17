package com.teczo.khatabill.ui.parties

import android.widget.Toast
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teczo.khatabill.R
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.domain.model.PaymentType
import com.teczo.khatabill.ui.components.AccentButton
import com.teczo.khatabill.ui.components.ScreenHeader
import com.teczo.khatabill.ui.components.TealCard
import com.teczo.khatabill.ui.components.WhiteCard
import com.teczo.khatabill.ui.util.Labels
import com.teczo.khatabill.util.dates.DateFormat
import com.teczo.khatabill.util.money.MoneyFormatter
import com.teczo.khatabill.util.pdf.PdfShare
import com.teczo.khatabill.util.pdf.StatementPdfBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@Composable
fun PartyDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onAddPayment: (String) -> Unit,
    onRemind: (String) -> Unit,
    onOpenInvoice: (String) -> Unit,
    viewModel: PartyDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val business by viewModel.business.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val failed = stringResource(R.string.invoice_pdf_failed)
    val party = state.party

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        ScreenHeader(party?.name.orEmpty(), onBack)
        if (party == null) return@Column
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TealCard(Modifier.fillMaxWidth()) {
                val label = when {
                    state.balance > 0 -> R.string.party_balance_to_collect
                    state.balance < 0 -> R.string.party_balance_to_pay
                    else -> R.string.party_balance_settled
                }
                Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
                if (state.balance != 0L) {
                    Text(
                        stringResource(R.string.amount_rupees, MoneyFormatter.format(abs(state.balance))),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                }
                party.phone?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp)) }
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentButton(onClick = { onAddPayment(party.id) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.party_add_payment), style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = {
                            val b = business ?: return@OutlinedButton
                            val st = state.statement ?: return@OutlinedButton
                            scope.launch {
                                try {
                                    val file = withContext(Dispatchers.IO) { StatementPdfBuilder(context).build(b, party, st) }
                                    PdfShare.shareFile(context, file)
                                } catch (e: Exception) {
                                    Toast.makeText(context, failed, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    ) { Text(stringResource(R.string.party_statement), style = MaterialTheme.typography.labelLarge) }
                }
                if (state.balance > 0) {
                    OutlinedButton(
                        onClick = { onRemind(party.id) },
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    ) { Text(stringResource(R.string.party_remind), style = MaterialTheme.typography.labelLarge) }
                }
                OutlinedButton(
                    onClick = { onEdit(party.id) },
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) { Text(stringResource(R.string.btn_edit), style = MaterialTheme.typography.labelLarge) }
            }

            Text(stringResource(R.string.party_section_bills), style = MaterialTheme.typography.titleLarge)
            if (state.invoices.isEmpty()) Text(stringResource(R.string.party_no_bills), style = MaterialTheme.typography.bodyMedium)
            state.invoices.forEach { inv ->
                WhiteCard(Modifier.fillMaxWidth().clickable { onOpenInvoice(inv.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(inv.number, style = MaterialTheme.typography.titleMedium)
                            Text(DateFormat.format(inv.date), style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.amount_rupees, MoneyFormatter.format(inv.grandTotal)), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(Labels.status(inv.status)), style = MaterialTheme.typography.bodySmall,
                                color = if (inv.status == InvoiceStatus.CANCELLED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Text(stringResource(R.string.party_section_payments), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
            if (state.payments.isEmpty()) Text(stringResource(R.string.party_no_payments), style = MaterialTheme.typography.bodyMedium)
            state.payments.forEach { p ->
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(if (p.type == PaymentType.RECEIVED) R.string.payment_received else R.string.payment_paid) + " · " + stringResource(Labels.paymentMode(p.mode)),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(DateFormat.format(p.date), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(stringResource(R.string.amount_rupees, MoneyFormatter.format(p.amount)), style = MaterialTheme.typography.titleMedium)
                }
                HorizontalDivider()
            }
        }
    }
}
