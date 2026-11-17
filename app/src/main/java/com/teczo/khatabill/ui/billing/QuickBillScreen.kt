package com.teczo.khatabill.ui.billing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teczo.khatabill.R
import com.teczo.khatabill.domain.model.GstRates
import com.teczo.khatabill.domain.model.GstScheme
import com.teczo.khatabill.domain.model.PaymentMode
import com.teczo.khatabill.ui.components.AccentButton
import com.teczo.khatabill.ui.components.AppTextField
import com.teczo.khatabill.ui.components.ScreenHeader
import com.teczo.khatabill.ui.util.Labels
import com.teczo.khatabill.util.money.MoneyFormatter
import com.teczo.khatabill.util.money.MoneyInput

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickBillScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: QuickBillViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val business by viewModel.business.collectAsStateWithLifecycle()
    val calc by viewModel.calc.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val savedId by viewModel.savedId.collectAsStateWithLifecycle()
    val defaultName = stringResource(R.string.quick_bill_default_name)

    LaunchedEffect(defaultName) { viewModel.setLineName(defaultName) }
    LaunchedEffect(savedId) { savedId?.let(onSaved) }

    val total = (calc as? CalcState.Ok)?.result?.grandTotal
    val amountBad = form.amount.isNotBlank() && MoneyInput.parsePaise(form.amount) == null
    val charges = business?.gstScheme == GstScheme.REGULAR

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        ScreenHeader(stringResource(R.string.quick_bill_title), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppTextField(
                form.amount,
                { v -> viewModel.update { it.copy(amount = v) } },
                stringResource(R.string.quick_bill_amount),
                keyboardType = KeyboardType.Decimal,
                error = if (amountBad) R.string.err_number else null,
                supportingText = if (charges) stringResource(R.string.quick_bill_incl_hint) else null,
            )
            AppTextField(
                form.description,
                { v -> viewModel.update { it.copy(description = v) } },
                stringResource(R.string.quick_bill_description),
            )
            if (charges) {
                Text(stringResource(R.string.field_gst_rate), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GstRates.options.forEach { bp ->
                        FilterChip(
                            selected = form.gstRateBp == bp,
                            onClick = { viewModel.update { it.copy(gstRateBp = bp) } },
                            label = { Text(stringResource(R.string.percent_value, MoneyInput.bpToText(bp))) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
            }
            Text(stringResource(R.string.field_paid_now), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(PaymentMode.CASH, PaymentMode.UPI, PaymentMode.CARD).forEach { m ->
                    FilterChip(
                        selected = form.paymentMode == m,
                        onClick = { viewModel.update { it.copy(paymentMode = m) } },
                        label = { Text(stringResource(Labels.paymentMode(m)), style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
            if (total != null) {
                Text(
                    stringResource(R.string.quick_bill_total, MoneyFormatter.format(total)),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            error?.let {
                Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
        AccentButton(
            onClick = viewModel::save,
            enabled = calc is CalcState.Ok,
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 56.dp),
        ) { Text(stringResource(R.string.btn_save_bill)) }
    }
}
