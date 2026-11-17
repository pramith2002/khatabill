package com.teczo.khatabill.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.data.entities.ItemEntity
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.data.repositories.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ItemRow(val item: ItemEntity, val stock: Long) {
    /** Low when a threshold is set and stock is at or below it. */
    val isLow: Boolean get() = item.minStock > 0 && stock <= item.minStock
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ItemListViewModel @Inject constructor(
    business: BusinessRepository,
    items: ItemRepository,
) : ViewModel() {
    val query = MutableStateFlow("")

    val rows: StateFlow<List<ItemRow>> = combine(business.active, query) { b, q -> b.id to q }
        .flatMapLatest { (businessId, q) ->
            combine(items.observe(businessId, q), items.observeAllStock(businessId)) { list, stocks ->
                val byItem = stocks.associate { it.itemId to it.qty }
                list.map { ItemRow(it, byItem[it.id] ?: 0L) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
