package com.teczo.khatabill.ui.reports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.teczo.khatabill.R
import com.teczo.khatabill.ui.components.PeachCard
import com.teczo.khatabill.ui.components.ScreenHeader
import com.teczo.khatabill.ui.components.WhiteCard

/** The list of every report. GST summaries get a peach tile so an accountant's reports are easy to find. */
@Composable
fun ReportHubScreen(onBack: () -> Unit, onOpen: (ReportKind) -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        ScreenHeader(stringResource(R.string.more_reports), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.reports_hint), style = MaterialTheme.typography.bodyMedium)
            ReportKind.entries.forEach { kind ->
                val modifier = Modifier.fillMaxWidth().clickable { onOpen(kind) }
                val content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {
                    Text(stringResource(kind.titleRes), style = MaterialTheme.typography.titleMedium, modifier = Modifier.heightIn(min = 32.dp))
                }
                if (kind.isGst) PeachCard(modifier, content) else WhiteCard(modifier, content)
            }
        }
    }
}
