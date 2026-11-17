package com.teczo.khatabill.ui.parties

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teczo.khatabill.R
import com.teczo.khatabill.ui.components.AddFab
import com.teczo.khatabill.ui.components.AppTextField
import com.teczo.khatabill.ui.components.EmptyState

@Composable
fun PartyListScreen(onAdd: () -> Unit, onOpen: (String) -> Unit, viewModel: PartyListViewModel = hiltViewModel()) {
    val parties by viewModel.items.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            Text(stringResource(R.string.nav_parties), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
            AppTextField(
                value = query,
                onValueChange = { viewModel.query.value = it },
                label = stringResource(R.string.search_hint),
                supportingText = stringResource(R.string.parties_hint),
                capitalization = KeyboardCapitalization.None,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            if (parties.isEmpty() && query.isBlank()) {
                EmptyState(stringResource(R.string.parties_empty), stringResource(R.string.parties_empty_hint))
            } else {
                LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)) {
                    items(parties, key = { it.id }) { party ->
                        Column(
                            Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { onOpen(party.id) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(party.name, style = MaterialTheme.typography.titleMedium)
                            party.nameKn?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            party.phone?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
        AddFab(stringResource(R.string.party_add), onAdd, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
}
