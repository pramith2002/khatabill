package com.teczo.khatabill.ui.parties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.data.entities.PartyEntity
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.data.repositories.LedgerRepository
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

enum class PartyFilter { ALL, TO_COLLECT, TO_PAY, OVERDUE }

/** A party with its derived balance (positive = they owe us). */
data class PartyRow(val party: PartyEntity, val balance: Long, val overdue: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PartyListViewModel @Inject constructor(
    business: BusinessRepository,
    parties: PartyRepository,
    ledger: LedgerRepository,
) : ViewModel() {
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(PartyFilter.ALL)

    val rows: StateFlow<List<PartyRow>> = combine(business.active, query) { b, q -> b.id to q }
        .flatMapLatest { (businessId, q) ->
            combine(parties.observe(businessId, q), ledger.observeBalances(businessId), filter) { list, balances, f ->
                list.map { PartyRow(it, balances.byParty[it.id] ?: 0L, it.id in balances.overdueParties) }
                    .filter {
                        when (f) {
                            PartyFilter.ALL -> true
                            PartyFilter.TO_COLLECT -> it.balance > 0
                            PartyFilter.TO_PAY -> it.balance < 0
                            PartyFilter.OVERDUE -> it.overdue
                        }
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
