package com.teczo.khatabill.ui.items

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.data.entities.ItemEntity
import com.teczo.khatabill.data.entities.StockMovementEntity
import com.teczo.khatabill.data.repositories.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val items: ItemRepository,
) : ViewModel() {
    private val itemId: String = checkNotNull(savedState["itemId"])

    val item: StateFlow<ItemEntity?> =
        items.observeById(itemId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val stock: StateFlow<Long> =
        items.observeStock(itemId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)
    val movements: StateFlow<List<StockMovementEntity>> =
        items.observeMovements(itemId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** [qtyDelta] is positive to add stock, negative to remove it (thousandths). */
    fun adjust(qtyDelta: Long, note: String) {
        val current = item.value ?: return
        viewModelScope.launch {
            items.adjustStock(current, qtyDelta, LocalDate.now().toEpochDay(), note.trim().ifBlank { null })
        }
    }
}
