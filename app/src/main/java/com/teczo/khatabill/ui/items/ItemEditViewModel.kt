package com.teczo.khatabill.ui.items

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.R
import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.data.entities.CategoryEntity
import com.teczo.khatabill.data.entities.ItemEntity
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.data.repositories.CategoryRepository
import com.teczo.khatabill.data.repositories.ItemRepository
import com.teczo.khatabill.domain.model.GstScheme
import com.teczo.khatabill.domain.model.ItemType
import com.teczo.khatabill.domain.model.UnitCatalog
import com.teczo.khatabill.util.money.MoneyInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

const val NEW_ITEM_ID = "new"

enum class ItemField { NAME, UNIT, SALE, PURCHASE, MRP, MIN_STOCK, OPENING, BARCODE }

data class ItemForm(
    val name: String = "",
    val nameKn: String = "",
    val type: ItemType = ItemType.PRODUCT,
    val unit: String = "",
    val barcode: String = "",
    val sku: String = "",
    val hsn: String = "",
    val gstRateBp: Int = 0,
    val taxInclusive: Boolean = false,
    val salePrice: String = "",
    val purchasePrice: String = "",
    val mrp: String = "",
    val minStock: String = "",
    val openingStock: String = "",
    val categoryId: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ItemEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val businessRepository: BusinessRepository,
    private val items: ItemRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {
    private val itemId: String = savedState["itemId"] ?: NEW_ITEM_ID
    val isNew = itemId == NEW_ITEM_ID

    private val _form = MutableStateFlow(ItemForm())
    val form: StateFlow<ItemForm> = _form.asStateFlow()
    private val _errors = MutableStateFlow<Map<ItemField, Int>>(emptyMap())
    val errors: StateFlow<Map<ItemField, Int>> = _errors.asStateFlow()
    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    private val _business = MutableStateFlow<BusinessEntity?>(null)
    val business: StateFlow<BusinessEntity?> = _business.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = businessRepository.active
        .flatMapLatest { categoryRepository.observeAll(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var original: ItemEntity? = null

    init {
        viewModelScope.launch {
            val biz = businessRepository.active.first()
            _business.value = biz
            if (isNew) {
                _form.update { it.copy(unit = UnitCatalog.unitsFor(biz.businessType).first()) }
            } else {
                items.get(itemId)?.let { item ->
                    original = item
                    _form.value = ItemForm(
                        name = item.name, nameKn = item.nameKn.orEmpty(), type = item.type, unit = item.unit,
                        barcode = item.barcode.orEmpty(), sku = item.sku.orEmpty(), hsn = item.hsnSac.orEmpty(),
                        gstRateBp = item.gstRateBp, taxInclusive = item.taxInclusive,
                        salePrice = MoneyInput.paiseToText(item.salePrice),
                        purchasePrice = if (item.purchasePrice == 0L) "" else MoneyInput.paiseToText(item.purchasePrice),
                        mrp = item.mrp?.let { MoneyInput.paiseToText(it) }.orEmpty(),
                        minStock = if (item.minStock == 0L) "" else MoneyInput.qtyToText(item.minStock),
                        categoryId = item.categoryId,
                    )
                }
            }
        }
    }

    /** Units offered in the picker: the business-type list plus the item's current unit if it is not in it. */
    fun unitChoices(): List<String> {
        val base = _business.value?.let { UnitCatalog.unitsFor(it.businessType) } ?: UnitCatalog.all
        val current = _form.value.unit
        return if (current.isBlank() || current in base) base else base + current
    }

    val chargesGst: Boolean get() = _business.value?.gstScheme == GstScheme.REGULAR

    fun update(change: (ItemForm) -> ItemForm) {
        _form.update(change)
        _errors.value = emptyMap()
    }

    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val biz = businessRepository.active.first()
            val created = categoryRepository.add(biz.id, name)
            _form.update { it.copy(categoryId = created.id) }
        }
    }

    fun save() {
        val f = _form.value
        val errors = buildMap<ItemField, Int> {
            if (f.name.isBlank()) put(ItemField.NAME, R.string.err_required)
            if (f.unit.isBlank()) put(ItemField.UNIT, R.string.err_required)
            if (f.salePrice.isNotBlank() && MoneyInput.parsePaise(f.salePrice) == null) put(ItemField.SALE, R.string.err_number)
            if (f.purchasePrice.isNotBlank() && MoneyInput.parsePaise(f.purchasePrice) == null) put(ItemField.PURCHASE, R.string.err_number)
            if (f.mrp.isNotBlank() && MoneyInput.parsePaise(f.mrp) == null) put(ItemField.MRP, R.string.err_number)
            if (f.minStock.isNotBlank() && MoneyInput.parseQty(f.minStock) == null) put(ItemField.MIN_STOCK, R.string.err_number)
            if (isNew && f.openingStock.isNotBlank() && MoneyInput.parseQty(f.openingStock) == null) put(ItemField.OPENING, R.string.err_number)
        }
        if (errors.isNotEmpty()) {
            _errors.value = errors
            return
        }
        viewModelScope.launch {
            val biz = businessRepository.active.first()
            val barcode = f.barcode.trim()
            if (barcode.isNotEmpty()) {
                val clash = items.findByBarcode(biz.id, barcode)
                if (clash != null && clash.id != itemId) {
                    _errors.value = mapOf(ItemField.BARCODE to R.string.err_barcode_exists)
                    return@launch
                }
            }
            val now = System.currentTimeMillis()
            val base = original ?: ItemEntity(
                id = UUID.randomUUID().toString(), businessId = biz.id, createdAt = now, updatedAt = now,
                name = "", unit = f.unit,
            )
            val item = base.copy(
                name = f.name.trim(),
                nameKn = f.nameKn.trim().ifBlank { null },
                type = f.type,
                unit = f.unit,
                barcode = barcode.ifBlank { null },
                sku = f.sku.trim().ifBlank { null },
                hsnSac = f.hsn.trim().ifBlank { null },
                gstRateBp = if (chargesGst) f.gstRateBp else 0,
                taxInclusive = chargesGst && f.taxInclusive,
                salePrice = MoneyInput.parsePaise(f.salePrice) ?: 0L,
                purchasePrice = MoneyInput.parsePaise(f.purchasePrice) ?: 0L,
                mrp = MoneyInput.parsePaise(f.mrp),
                minStock = MoneyInput.parseQty(f.minStock) ?: 0L,
                categoryId = f.categoryId,
            )
            if (isNew) {
                val opening = if (f.type == ItemType.PRODUCT) MoneyInput.parseQty(f.openingStock) ?: 0L else 0L
                items.create(item, opening, LocalDate.now().toEpochDay())
            } else {
                items.update(item)
            }
            _done.value = true
        }
    }
}
