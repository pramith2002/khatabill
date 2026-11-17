package com.teczo.khatabill.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.teczo.khatabill.domain.model.ItemType
import com.teczo.khatabill.domain.model.SyncState

/**
 * A product or service. Stock is NOT stored here: it is the sum of StockMovement rows
 * (opening stock is written as an OPENING movement when the item is created).
 */
@Entity(
    tableName = "item",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["businessId", "name"]),
        Index(value = ["businessId", "barcode"]),
        Index(value = ["categoryId"]),
    ],
)
data class ItemEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING,

    val name: String,
    val nameKn: String? = null,
    val type: ItemType = ItemType.PRODUCT,
    val sku: String? = null,
    val barcode: String? = null,
    val hsnSac: String? = null,
    /** Unit code such as "kg", "pcs", "box". */
    val unit: String,
    val altUnit: String? = null,
    /** How many [unit]s in one [altUnit], in thousandths (1 carton = 24 packs -> 24000). */
    val altUnitFactor: Long? = null,
    val salePrice: Long = 0,
    val purchasePrice: Long = 0,
    val mrp: Long? = null,
    /** Basis points: 18% = 1800. */
    val gstRateBp: Int = 0,
    val taxInclusive: Boolean = false,
    /** Thousandths of [unit]. Low-stock alert threshold. */
    val minStock: Long = 0,
    val categoryId: String? = null,
    val imageUri: String? = null,
    val batchTracking: Boolean = false,
)
