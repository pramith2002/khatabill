package com.teczo.khatabill.ui.credit

import android.widget.Toast
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teczo.khatabill.R
import com.teczo.khatabill.domain.model.ReminderChannel
import com.teczo.khatabill.ui.components.AccentButton
import com.teczo.khatabill.ui.components.AppTextField
import com.teczo.khatabill.ui.components.ScreenHeader
import com.teczo.khatabill.ui.components.TealCard
import com.teczo.khatabill.util.money.MoneyFormatter
import com.teczo.khatabill.util.pdf.StatementPdfBuilder
import com.teczo.khatabill.util.locale.LocalizedContext
import com.teczo.khatabill.util.share.WhatsAppShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Compose a collection reminder in Kannada, English or Hindi and open WhatsApp (or SMS) with it. The user taps send. */
@Composable
fun ReminderScreen(onBack: () -> Unit, viewModel: ReminderViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val noPhone = stringResource(R.string.reminder_no_phone)
    val failed = stringResource(R.string.invoice_pdf_failed)
    val party = s.party

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        ScreenHeader(stringResource(R.string.reminder_title), onBack)
        if (party == null) return@Column
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TealCard(Modifier.fillMaxWidth()) {
                Text(party.name, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.amount_rupees, MoneyFormatter.format(s.owed)), style = MaterialTheme.typography.headlineMedium)
                Text(party.phone ?: noPhone, style = MaterialTheme.typography.bodyMedium)
            }
            Text(stringResource(R.string.reminder_language), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("kn" to R.string.lang_kn, "en" to R.string.lang_en, "hi" to R.string.lang_hi).forEach { (tag, label) ->
                    FilterChip(
                        selected = s.language == tag, onClick = { viewModel.setLanguage(tag) },
                        label = { Text(stringResource(label), style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = s.style == ReminderStyle.GENTLE, onClick = { viewModel.setStyle(ReminderStyle.GENTLE) },
                    label = { Text(stringResource(R.string.reminder_style_gentle), style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
                FilterChip(
                    selected = s.style == ReminderStyle.FIRM, onClick = { viewModel.setStyle(ReminderStyle.FIRM) },
                    label = { Text(stringResource(R.string.reminder_style_firm), style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
            AppTextField(s.message, viewModel::setMessage, stringResource(R.string.reminder_message), singleLine = false)
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.reminder_attach_statement), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = s.attachStatement, onCheckedChange = viewModel::setAttach)
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AccentButton(
                onClick = {
                    val biz = s.business ?: return@AccentButton
                    scope.launch {
                        try {
                            val statement = s.statement
                            val pdf = if (s.attachStatement && statement != null) {
                                withContext(Dispatchers.IO) { StatementPdfBuilder(LocalizedContext.of(context, s.language)).build(biz, party, statement) }
                            } else null
                            WhatsAppShare.send(context, party.whatsapp ?: party.phone, s.message, pdf)
                            viewModel.recordSent(ReminderChannel.WHATSAPP)
                        } catch (e: Exception) {
                            Toast.makeText(context, failed, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.reminder_whatsapp)) }
            OutlinedButton(
                onClick = {
                    val phone = party.phone
                    if (phone.isNullOrBlank()) {
                        Toast.makeText(context, noPhone, Toast.LENGTH_LONG).show()
                    } else {
                        WhatsAppShare.sendSms(context, phone, s.message)
                        viewModel.recordSent(ReminderChannel.SMS)
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text(stringResource(R.string.reminder_sms)) }
        }
    }
}
