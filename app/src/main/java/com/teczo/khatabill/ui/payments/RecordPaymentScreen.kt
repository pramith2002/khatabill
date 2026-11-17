package com.teczo.khatabill.ui.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teczo.khatabill.R
import com.teczo.khatabill.domain.model.PaymentMode
import com.teczo.khatabill.domain.model.PaymentType
import com.teczo.khatabill.ui.components.AccentButton
import com.teczo.khatabill.ui.components.AppTextField
import com.teczo.khatabill.ui.components.Choice
import com.teczo.khatabill.ui.components.ChoiceSheet
import com.teczo.khatabill.ui.components.DateField
import com.teczo.khatabill.ui.components.PeachCard
import com.teczo.khatabill.ui.components.PickerField
import com.teczo.khatabill.ui.components.QrImage
import com.teczo.khatabill.ui.components.RadioRow
import com.teczo.khatabill.ui.components.ScreenHeader
import com.teczo.khatabill.ui.util.Labels
import com.teczo.khatabill.util.money.MoneyFormatter
import com.teczo.khatabill.util.money.MoneyInput
import com.teczo.khatabill.util.upi.UpiIntent
import com.teczo.khatabill.util.upi.UpiLink
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordPaymentScreen(onDone: () -> Unit, viewModel: RecordPaymentViewModel = hiltViewModel()) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val business by viewModel.business.collectAsStateWithLifecycle()
    val parties by viewModel.parties.collectAsStateWithLifecycle()
    val openInvoices by viewModel.openInvoices.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showParty by remember { mutableStateOf(false) }
    var showInvoice by remember { mutableStateOf(false) }

    LaunchedEffect(done) { if (done) onDone() }

    val party = parties.firstOrNull { it.id == form.partyId }
    val amountPaise = MoneyInput.parsePaise(form.amount)

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        ScreenHeader(stringResource(R.string.payment_title_new), onDone)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PickerField(
                label = stringResource(R.string.field_party),
                value = party?.name ?: "",
                onClick = { showParty = true },
                enabled = !viewModel.partyIsFixed,
                error = if (error == R.string.err_choose_party) R.string.err_choose_party else null,
            )
            if (balance != 0L) {
                val text = stringResource(R.string.amount_rupees, MoneyFormatter.format(abs(balance))) + " · " +
                    stringResource(if (balance > 0) R.string.party_balance_to_collect else R.string.party_balance_to_pay)
                Text(stringResource(R.string.balance_outstanding, text), style = MaterialTheme.typography.bodyMedium)
            }
            Row {
                RadioRow(stringResource(R.string.payment_received), form.type == PaymentType.RECEIVED, { viewModel.update { it.copy(type = PaymentType.RECEIVED, invoiceId = null) } }, Modifier.weight(1f))
                RadioRow(stringResource(R.string.payment_paid), form.type == PaymentType.PAID, { viewModel.update { it.copy(type = PaymentType.PAID, invoiceId = null) } }, Modifier.weight(1f))
            }
            AppTextField(
                form.amount, { v -> viewModel.update { it.copy(amount = v) } }, stringResource(R.string.field_amount),
                keyboardType = KeyboardType.Decimal,
                error = when {
                    error == R.string.err_amount -> R.string.err_amount
                    form.amount.isNotBlank() && amountPaise == null -> R.string.err_number
                    else -> null
                },
            )
            Text(stringResource(R.string.field_payment_mode), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentMode.entries.forEach { m ->
                    FilterChip(
                        selected = form.mode == m,
                        onClick = { viewModel.update { it.copy(mode = m) } },
                        label = { Text(stringResource(Labels.paymentMode(m)), style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }

            val upiId = business?.upiId
            if (form.mode == PaymentMode.UPI && form.type == PaymentType.RECEIVED && !upiId.isNullOrBlank() && amountPaise != null && amountPaise > 0) {
                val link = UpiLink.build(upiId, business?.name.orEmpty(), amountPaise, openInvoices.firstOrNull { it.id == form.invoiceId }?.number ?: business?.name.orEmpty())
                PeachCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.upi_scan_title), style = MaterialTheme.typography.titleMedium)
                    QrImage(link, Modifier.padding(vertical = 12.dp).size(220.dp).align(Alignment.CenterHorizontally))
                    Text(stringResource(R.string.amount_rupees, MoneyFormatter.format(amountPaise)), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
                    AccentButton(onClick = { UpiIntent.open(context, link) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(stringResource(R.string.upi_pay_with_app))
                    }
                }
            }

            DateField(stringResource(R.string.field_date), form.date, { d -> d?.let { v -> viewModel.update { it.copy(date = v) } } })
            if (openInvoices.isNotEmpty()) {
                PickerField(
                    stringResource(R.string.field_against_invoice),
                    openInvoices.firstOrNull { it.id == form.invoiceId }?.number ?: stringResource(R.string.against_oldest),
                    { showInvoice = true },
                )
            }
            AppTextField(form.reference, { v -> viewModel.update { it.copy(reference = v) } }, stringResource(R.string.field_reference))
            AppTextField(form.notes, { v -> viewModel.update { it.copy(notes = v) } }, stringResource(R.string.field_notes), singleLine = false)
        }
        AccentButton(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.btn_save_payment))
        }
    }

    if (showParty) {
        ChoiceSheet(
            title = stringResource(R.string.pick_party_for_payment),
            choices = parties.map { Choice(it.id, listOfNotNull(it.name, it.nameKn).joinToString(" · ")) },
            selectedKey = form.partyId,
            onSelect = { id -> parties.firstOrNull { it.id == id }?.let(viewModel::setParty); showParty = false },
            onDismiss = { showParty = false },
            searchable = true,
        )
    }
    if (showInvoice) {
        ChoiceSheet(
            title = stringResource(R.string.field_against_invoice),
            choices = listOf(Choice("", stringResource(R.string.against_oldest))) +
                openInvoices.map { Choice(it.id, it.number + " · ₹" + MoneyFormatter.format(it.grandTotal - it.paidAmount)) },
            selectedKey = form.invoiceId ?: "",
            onSelect = { id -> viewModel.update { it.copy(invoiceId = id.ifBlank { null }) }; showInvoice = false },
            onDismiss = { showInvoice = false },
        )
    }
}
