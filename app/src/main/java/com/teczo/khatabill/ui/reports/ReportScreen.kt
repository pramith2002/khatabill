package com.teczo.khatabill.ui.reports

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teczo.khatabill.R
import com.teczo.khatabill.ui.components.AccentButton
import com.teczo.khatabill.ui.components.RangePicker
import com.teczo.khatabill.ui.components.ScreenHeader
import com.teczo.khatabill.util.dates.DateFormat
import com.teczo.khatabill.util.export.CsvExport
import com.teczo.khatabill.util.pdf.PdfShare
import com.teczo.khatabill.util.pdf.ReportPdfBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ReportScreen(onBack: () -> Unit, viewModel: ReportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val business by viewModel.business.collectAsStateWithLifecycle()
    val kind by viewModel.selector.kind.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val scope = rememberCoroutineScope()
    val failed = stringResource(R.string.invoice_pdf_failed)
    val report = viewModel.kind
    val range = state.range

    fun rangeText() = range?.takeIf { report.usesRange }?.let { DateFormat.format(it.from) + " – " + DateFormat.format(it.to) }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        ScreenHeader(stringResource(report.titleRes), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (report.usesRange && range != null) {
                RangePicker(
                    kind = kind, range = range,
                    onKind = viewModel.selector::select, onCustom = { viewModel.selector.custom.value = it },
                )
                Text(rangeText().orEmpty(), style = MaterialTheme.typography.bodyMedium)
            }
            state.tables.forEach { ReportTableView(it) }
            if (report.isGst) Text(stringResource(R.string.report_gst_note), style = MaterialTheme.typography.bodySmall)
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    val b = business ?: return@OutlinedButton
                    scope.launch {
                        try {
                            val file = withContext(Dispatchers.IO) {
                                val headers = listOfNotNull(b.name, resources.getString(report.titleRes), rangeText())
                                CsvExport.write(File(File(context.cacheDir, "invoices"), "report-${report.name.lowercase()}.csv"), state.tables, { resources.getString(it) }, headers)
                            }
                            PdfShare.shareFile(context, file, "text/csv")
                        } catch (e: Exception) {
                            Toast.makeText(context, failed, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            ) { Text(stringResource(R.string.export_csv)) }
            AccentButton(
                onClick = {
                    val b = business ?: return@AccentButton
                    scope.launch {
                        try {
                            val file = withContext(Dispatchers.IO) {
                                ReportPdfBuilder(context).build(
                                    b, report.titleRes, rangeText(), state.tables, if (report.isGst) R.string.report_gst_note else null,
                                    File(File(context.cacheDir, "invoices"), "report-${report.name.lowercase()}.pdf"),
                                )
                            }
                            PdfShare.shareFile(context, file)
                        } catch (e: Exception) {
                            Toast.makeText(context, failed, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.export_pdf)) }
        }
    }
}
