package com.teczo.khatabill.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.teczo.khatabill.R
import com.teczo.khatabill.util.dates.DateFormat

/**
 * A tappable date box that opens the calendar. [epochDay] null means "no date" (shown as a dash).
 * When [clearable] is true the dialog offers a Clear button that reports null.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    epochDay: Long?,
    onChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    clearable: Boolean = false,
) {
    var show by remember { mutableStateOf(false) }
    PickerField(label, epochDay?.let { DateFormat.format(it) } ?: "", { show = true }, modifier)
    if (show) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (epochDay ?: DateFormat.today()) * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(it / 86_400_000L) }
                    show = false
                }) { Text(stringResource(R.string.btn_ok)) }
            },
            dismissButton = {
                Row2(clearable, onClear = { onChange(null); show = false }, onCancel = { show = false })
            },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun Row2(clearable: Boolean, onClear: () -> Unit, onCancel: () -> Unit) {
    androidx.compose.foundation.layout.Row {
        if (clearable) TextButton(onClick = onClear) { Text(stringResource(R.string.btn_clear)) }
        TextButton(onClick = onCancel) { Text(stringResource(R.string.btn_cancel)) }
    }
}
