package com.teczo.khatabill.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

data class Choice(val key: String, val label: String)

/** A tappable box that shows a label and the current value; opens a [ChoiceSheet] (no tiny dropdowns). */
@Composable
fun PickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes error: Int? = null,
    enabled: Boolean = true,
    supportingText: String? = null,
) {
    Column(modifier.fillMaxWidth()) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(enabled = enabled, onClick = onClick),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Text(
                    value.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val line = when {
            error != null -> stringResource(error)
            else -> supportingText
        }
        if (line != null) {
            Text(
                line,
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

/** A full-width radio row, at least 56 dp tall. */
@Composable
fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

/** Bottom sheet list with optional search. Rows are at least 56 dp tall. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoiceSheet(
    title: String,
    choices: List<Choice>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    searchable: Boolean = false,
    footer: (@Composable () -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val shown = if (query.isBlank()) choices else choices.filter { it.label.contains(query.trim(), ignoreCase = true) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            if (searchable) {
                AppTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = stringResource(com.teczo.khatabill.R.string.search_hint),
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None,
                )
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp), modifier = Modifier.weight(1f, fill = false)) {
                items(shown, key = { it.key }) { choice ->
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .clickable { onSelect(choice.key) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(choice.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (choice.key == selectedKey) Icon(Icons.Filled.Check, contentDescription = null)
                    }
                }
            }
            footer?.invoke()
        }
    }
}
