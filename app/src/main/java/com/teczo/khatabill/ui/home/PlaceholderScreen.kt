package com.teczo.khatabill.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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

/** Empty-state style placeholder for tabs that are built in later phases. [extra] is shown below the hint. */
@Composable
fun PlaceholderScreen(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    extra: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.coming_soon), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.coming_soon_hint), style = MaterialTheme.typography.bodyMedium)
        extra()
    }
}
