package com.teczo.khatabill.ui.parties

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teczo.khatabill.R
import com.teczo.khatabill.domain.model.BalanceType
import com.teczo.khatabill.domain.model.PartyType
import com.teczo.khatabill.ui.components.AppTextField
import com.teczo.khatabill.ui.components.Choice
import com.teczo.khatabill.ui.components.ChoiceSheet
import com.teczo.khatabill.ui.components.PickerField
import com.teczo.khatabill.ui.components.RadioRow
import com.teczo.khatabill.ui.components.ScreenHeader
import com.teczo.khatabill.ui.util.Labels
import com.teczo.khatabill.util.validators.GstStateCodes

@Composable
fun PartyEditScreen(onDone: () -> Unit, viewModel: PartyEditViewModel = hiltViewModel()) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()
    var showState by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(done) { if (done) onDone() }

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        ScreenHeader(stringResource(if (viewModel.isNew) R.string.party_title_new else R.string.party_title_edit), onDone)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppTextField(form.name, { v -> viewModel.update { it.copy(name = v) } }, stringResource(R.string.field_party_name), error = errors[PartyField.NAME])
            AppTextField(form.nameKn, { v -> viewModel.update { it.copy(nameKn = v) } }, stringResource(R.string.field_name_kn))

            Text(stringResource(R.string.field_party_type), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    PartyType.CUSTOMER to R.string.party_type_customer,
                    PartyType.SUPPLIER to R.string.party_type_supplier,
                    PartyType.BOTH to R.string.party_type_both,
                ).forEach { (type, label) ->
                    RadioRow(stringResource(label), form.type == type, { viewModel.update { it.copy(type = type) } }, Modifier.weight(1f))
                }
            }

            AppTextField(
                form.phone, { v -> viewModel.update { it.copy(phone = v.filter(Char::isDigit).take(10)) } },
                stringResource(R.string.field_phone), error = errors[PartyField.PHONE], keyboardType = KeyboardType.Phone,
            )
            AppTextField(
                form.whatsapp, { v -> viewModel.update { it.copy(whatsapp = v.filter(Char::isDigit).take(10)) } },
                stringResource(R.string.field_whatsapp), error = errors[PartyField.WHATSAPP], keyboardType = KeyboardType.Phone,
            )
            AppTextField(
                form.gstin, { v -> viewModel.update { it.copy(gstin = v.take(15)) } }, stringResource(R.string.field_gstin_optional),
                error = errors[PartyField.GSTIN], capitalization = KeyboardCapitalization.Characters,
            )
            PickerField(
                label = stringResource(R.string.field_state_optional),
                value = Labels.state(form.stateCode)?.let { stringResource(it) } ?: "",
                onClick = { showState = true },
            )
            AppTextField(form.billingAddress, { v -> viewModel.update { it.copy(billingAddress = v) } }, stringResource(R.string.field_billing_address), singleLine = false)
            AppTextField(form.shippingAddress, { v -> viewModel.update { it.copy(shippingAddress = v) } }, stringResource(R.string.field_shipping_address), singleLine = false)

            AppTextField(
                form.openingBalance, { v -> viewModel.update { it.copy(openingBalance = v) } },
                stringResource(R.string.field_opening_balance), error = errors[PartyField.OPENING], keyboardType = KeyboardType.Decimal,
            )
            RadioRow(stringResource(R.string.balance_to_collect), form.balanceType == BalanceType.TO_COLLECT, { viewModel.update { it.copy(balanceType = BalanceType.TO_COLLECT) } })
            RadioRow(stringResource(R.string.balance_to_pay), form.balanceType == BalanceType.TO_PAY, { viewModel.update { it.copy(balanceType = BalanceType.TO_PAY) } })
            AppTextField(
                form.creditLimit, { v -> viewModel.update { it.copy(creditLimit = v) } },
                stringResource(R.string.field_credit_limit), error = errors[PartyField.CREDIT], keyboardType = KeyboardType.Decimal,
            )
            AppTextField(form.notes, { v -> viewModel.update { it.copy(notes = v) } }, stringResource(R.string.field_notes), singleLine = false)
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!viewModel.isNew) {
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.btn_delete))
                }
            }
            Button(onClick = viewModel::save, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                Text(stringResource(R.string.btn_save))
            }
        }
    }

    if (showState) {
        ChoiceSheet(
            title = stringResource(R.string.picker_choose_state),
            choices = GstStateCodes.codes.sorted().map { Choice(it, Labels.state(it)?.let { r -> stringResource(r) } ?: it) },
            selectedKey = form.stateCode.ifBlank { null },
            onSelect = { code -> viewModel.update { it.copy(stateCode = code) }; showState = false },
            onDismiss = { showState = false },
            searchable = true,
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.party_delete_title)) },
            text = { Text(stringResource(R.string.party_delete_message)) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text(stringResource(R.string.btn_delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.btn_cancel)) } },
        )
    }
}
