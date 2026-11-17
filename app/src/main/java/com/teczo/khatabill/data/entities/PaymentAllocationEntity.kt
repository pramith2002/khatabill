package com.teczo.khatabill.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.teczo.khatabill.domain.model.SyncState

/** How much of a payment was applied to one invoice. An invoice's paid amount is the sum of its allocations. */
@Entity(
    tableName = "payment_allocation",
    foreignKeys = [
        ForeignKey(entity = PaymentEntity::class, parentColumns = ["id"], childColumns = ["paymentId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = InvoiceEntity::class, parentColumns = ["id"], childColumns = ["invoiceId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index(value = ["paymentId"]), Index(value = ["invoiceId"])],
)
data class PaymentAllocationEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING,

    val paymentId: String,
    val invoiceId: String,
    val amount: Long,
)
