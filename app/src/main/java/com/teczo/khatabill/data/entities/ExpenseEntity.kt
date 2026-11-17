package com.teczo.khatabill.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.teczo.khatabill.domain.model.PaymentMode
import com.teczo.khatabill.domain.model.SyncState

@Entity(
    tableName = "expense",
    foreignKeys = [
        ForeignKey(entity = PartyEntity::class, parentColumns = ["id"], childColumns = ["partyId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index(value = ["businessId", "date"]), Index(value = ["partyId"])],
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING,

    /** Epoch day. */
    val date: Long,
    /** Category key such as "rent" or "transport" (see ExpenseCategories). */
    val category: String,
    val amount: Long,
    val mode: PaymentMode = PaymentMode.CASH,
    val partyId: String? = null,
    val notes: String? = null,
    /** Path of the receipt photo inside the app's private storage. */
    val receiptUri: String? = null,
)
