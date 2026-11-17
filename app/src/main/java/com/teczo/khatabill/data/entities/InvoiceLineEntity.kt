package com.teczo.khatabill.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.teczo.khatabill.domain.model.SyncState

/** A snapshot of one invoice line. Never changes when the item is edited later. */
@Entity(
    tableName = "invoice_line",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["invoiceId"]), Index(value = ["itemId"])],
)
data class InvoiceLineEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING,

    val invoiceId: String,
    val lineNo: Int,
    /** null for a free-text line with no item behind it. */
    val itemId: String? = null,
    val nameSnapshot: String,
    val hsnSac: String? = null,
    /** Thousandths of [unit]. */
    val qty: Long,
    val unit: String,
    /** Paise per unit; includes tax when [taxInclusive]. */
    val rate: Long,
    val taxInclusive: Boolean = false,
    /** Basis points; 0 when the line discount was a flat amount. */
    val discountPercentBp: Int = 0,
    /** Total discount on this line (own + share of overall discount), on the taxable basis. */
    val discountAmount: Long = 0,
    val taxableValue: Long,
    val gstRateBp: Int = 0,
    val cgst: Long = 0,
    val sgst: Long = 0,
    val igst: Long = 0,
    val lineTotal: Long,
)
