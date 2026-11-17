package com.teczo.khatabill.ui.onboarding

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teczo.khatabill.R

@Composable
fun BusinessSetupScreen(onFinished: () -> Unit, viewModel: BusinessSetupViewModel = hiltViewModel()) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val step by viewModel.step.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    LaunchedEffect(saved) { if (saved) onFinished() }

    val titleRes = when (step) {
        0 -> R.string.setup_title_shop
        1 -> R.string.setup_title_gst
        else -> R.string.setup_title_pay
    }
    val hintRes = when (step) {
        0 -> R.string.setup_hint_shop
        1 -> R.string.setup_hint_gst
        else -> R.string.setup_hint_pay
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.setup_step_of, step + 1, SETUP_STEPS), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(titleRes), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(hintRes), style = MaterialTheme.typography.bodyMedium)
            when (step) {
                0 -> StepShop(form, errors, viewModel::update)
                1 -> StepGstAndType(form, errors, viewModel::update)
                else -> StepPayments(form, errors, viewModel::update)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (step > 0) {
                OutlinedButton(onClick = viewModel::back, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.btn_back))
                }
            }
            Button(onClick = viewModel::next, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                Text(stringResource(if (step < SETUP_STEPS - 1) R.string.btn_next else R.string.btn_finish))
            }
        }
    }
}
