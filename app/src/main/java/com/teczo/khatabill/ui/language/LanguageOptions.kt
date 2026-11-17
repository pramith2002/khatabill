package com.teczo.khatabill.ui.language

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.teczo.khatabill.R

private data class LanguageChoice(val tag: String, @StringRes val label: Int)

private val choices = listOf(
    LanguageChoice("kn", R.string.lang_kn),
    LanguageChoice("en", R.string.lang_en),
    LanguageChoice("hi", R.string.lang_hi),
)

/** Three large buttons (56 dp tall, above the 48 dp minimum). The current language is filled and ticked. */
@Composable
fun LanguageOptions(
    current: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        choices.forEach { choice ->
            val buttonModifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            if (choice.tag == current) {
                Button(onClick = { onSelect(choice.tag) }, modifier = buttonModifier) {
                    Row {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = stringResource(R.string.language_selected),
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(stringResource(choice.label))
                    }
                }
            } else {
                OutlinedButton(onClick = { onSelect(choice.tag) }, modifier = buttonModifier) {
                    Text(stringResource(choice.label))
                }
            }
        }
    }
}
