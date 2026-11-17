package com.teczo.khatabill.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.teczo.khatabill.domain.model.ReminderChannel
import com.teczo.khatabill.domain.model.ReminderStatus
import com.teczo.khatabill.domain.model.SyncState

/** A collection reminder that was composed, sent or raised as a notification. */
@Entity(
    tableName = "reminder",
    foreignKeys = [
        ForeignKey(entity = PartyEntity::class, parentColumns = ["id"], childColumns = ["partyId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = InvoiceEntity::class, parentColumns = ["id"], childColumns = ["invoiceId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index(value = ["businessId", "scheduledAt"]), Index(value = ["partyId"]), Index(value = ["invoiceId"])],
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING,

    val partyId: String,
    val invoiceId: String? = null,
    val scheduledAt: Long,
    val channel: ReminderChannel,
    val status: ReminderStatus = ReminderStatus.PENDING,
    val message: String,
)
