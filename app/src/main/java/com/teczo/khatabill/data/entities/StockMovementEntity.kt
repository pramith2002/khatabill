package com.teczo.khatabill.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.teczo.khatabill.domain.model.StockReason
import com.teczo.khatabill.domain.model.SyncState

/** Stock of an item = sum of [qtyDelta] over its movements. Rows are only ever added, never edited. */
@Entity(
    tableName = "stock_movement",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["itemId"]), Index(value = ["refInvoiceId"]), Index(value = ["businessId"])],
)
data class StockMovementEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING,

    val itemId: String,
    /** Epoch day. */
    val date: Long,
    /** Thousandths of the item's unit; negative when stock goes out. */
    val qtyDelta: Long,
    val reason: StockReason,
    val refInvoiceId: String? = null,
    val note: String? = null,
)
