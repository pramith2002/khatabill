package com.teczo.khatabill.ui.billing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teczo.khatabill.R
import com.teczo.khatabill.ui.components.AccentButton
import com.teczo.khatabill.ui.components.AppTextField
import com.teczo.khatabill.ui.components.QrImage
import com.teczo.khatabill.ui.components.ScreenHeader
import com.teczo.khatabill.ui.components.TealCard
import com.teczo.khatabill.util.money.MoneyFormatter
import com.teczo.khatabill.util.upi.UpiIntent
import com.teczo.khatabill.util.upi.UpiLink
import com.teczo.khatabill.util.validators.FieldValidators

/** Full-screen UPI QR for the unpaid balance of one bill, so the customer can scan it from across the counter. */
@Composable
fun UpiQrScreen(onBack: () -> Unit, viewModel: UpiQrViewModel = hiltViewModel()) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val business by viewModel.business.collectAsStateWithLifecycle()
    val paid by viewModel.paid.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirm by remember { mutableStateOf(false) }

    LaunchedEffect(paid) { if (paid) onBack() }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        ScreenHeader(stringResource(R.string.upi_scan_title), onBack)
        val inv = data?.invoice
        val biz = business
        if (inv == null || biz == null) return@Column
        val due = inv.grandTotal - inv.paidAmount
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val upiId = biz.upiId
            if (upiId.isNullOrBlank()) {
                var typed by remember { mutableStateOf("") }
                Text(stringResource(R.string.upi_missing), style = MaterialTheme.typography.bodyLarge)
                AppTextField(
                    typed, { typed = it }, stringResource(R.string.field_upi), keyboardType = KeyboardType.Email,
                    capitalization = KeyboardCapitalization.None,
                    error = if (typed.isNotBlank() && !FieldValidators.isValidUpi(typed.trim())) R.string.err_upi else null,
                )
                AccentButton(onClick = { viewModel.saveUpiId(typed) }, enabled = FieldValidators.isValidUpi(typed.trim()), modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.btn_save))
                }
            } else if (due <= 0) {
                Text(stringResource(R.string.status_paid), style = MaterialTheme.typography.headlineMedium)
            } else {
                val link = UpiLink.build(upiId, biz.name, due, inv.number)
                TealCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.upi_for_bill, inv.number), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.amount_rupees, MoneyFormatter.format(due)), style = MaterialTheme.typography.displaySmall)
                    Text(biz.name, style = MaterialTheme.typography.bodyMedium)
                }
                QrImage(link, Modifier.size(280.dp))
                Text(upiId, style = MaterialTheme.typography.bodyMedium)
                AccentButton(onClick = { UpiIntent.open(context, link) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.upi_pay_with_app))
                }
                OutlinedButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.upi_mark_paid))
                }
            }
        }
        if (confirm) {
            val due2 = inv.grandTotal - inv.paidAmount
            AlertDialog(
                onDismissRequest = { confirm = false },
                title = { Text(stringResource(R.string.upi_confirm_title)) },
                text = { Text(stringResource(R.string.upi_confirm_message, stringResource(R.string.amount_rupees, MoneyFormatter.format(due2)))) },
                confirmButton = { TextButton(onClick = { confirm = false; viewModel.markPaid(due2) }) { Text(stringResource(R.string.upi_mark_paid)) } },
                dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.btn_cancel)) } },
            )
        }
    }
}
