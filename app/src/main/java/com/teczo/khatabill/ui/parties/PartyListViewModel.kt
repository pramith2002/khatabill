package com.teczo.khatabill.ui.parties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.data.entities.PartyEntity
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.data.repositories.PartyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PartyListViewModel @Inject constructor(
    business: BusinessRepository,
    parties: PartyRepository,
) : ViewModel() {
    val query = MutableStateFlow("")

    val items: StateFlow<List<PartyEntity>> = combine(business.active, query) { b, q -> b.id to q }
        .flatMapLatest { (businessId, q) -> parties.observe(businessId, q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
