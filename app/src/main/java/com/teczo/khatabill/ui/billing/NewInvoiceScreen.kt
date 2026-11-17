package com.teczo.khatabill.ui.billing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun NewInvoiceScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: NewInvoiceViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val business by viewModel.business.collectAsStateWithLifecycle()
    val calc by viewModel.calc.collectAsStateWithLifecycle()
    val parties by viewModel.parties.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val savedId by viewModel.savedId.collectAsStateWithLifecycle()

    LaunchedEffect(savedId) { savedId?.let(onSaved) }

    NewInvoiceContent(
        form = form,
        business = business,
        calc = calc,
        parties = parties,
        items = items,
        error = error,
        actions = NewInvoiceActions(
            onBack = onBack,
            onUpdate = viewModel::update,
            onSetParty = viewModel::setParty,
            onAddItem = viewModel::addItem,
            onUpdateLine = viewModel::updateLine,
            onRemoveLine = viewModel::removeLine,
            onSave = viewModel::save,
        ),
    )
}
