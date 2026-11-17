package com.teczo.khatabill.ui.language

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.teczo.khatabill.R

/** First screen of the app: pick Kannada, English or Hindi. Titles are shown in the current locale. */
@Composable
fun LanguagePickerScreen(onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.vyro_logo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.fillMaxWidth(0.8f).background(Color.White, RoundedCornerShape(16.dp)).padding(12.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.language_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.language_hint), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        LanguageOptions(current = "", onSelect = onSelect)
    }
}
