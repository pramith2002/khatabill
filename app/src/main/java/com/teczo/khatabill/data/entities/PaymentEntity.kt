package com.teczo.khatabill.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.teczo.khatabill.domain.model.PaymentMode
import com.teczo.khatabill.domain.model.PaymentType
import com.teczo.khatabill.domain.model.SyncState

/**
 * Money received from (or paid to) a party. Amounts are paise.
 * A payment may be split over several invoices (see [PaymentAllocationEntity]); whatever is not
 * allocated stays "on account" and still reduces the party's balance.
 */
@Entity(
    tableName = "payment",
    foreignKeys = [
        ForeignKey(entity = PartyEntity::class, parentColumns = ["id"], childColumns = ["partyId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index(value = ["businessId", "date"]), Index(value = ["partyId"]), Index(value = ["invoiceId"])],
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING,

    val type: PaymentType,
    /** null for a walk-in customer's payment taken at billing time. */
    val partyId: String? = null,
    /** Set only for money taken at billing time with this invoice; cancelling that invoice refunds it. */
    val invoiceId: String? = null,
    /** Epoch day. */
    val date: Long,
    val amount: Long,
    val mode: PaymentMode = PaymentMode.CASH,
    val reference: String? = null,
    val notes: String? = null,
)
