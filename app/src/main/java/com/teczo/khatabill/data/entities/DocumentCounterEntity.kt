package com.teczo.khatabill.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.teczo.khatabill.domain.model.SyncState

/**
 * Last sequence number handed out per business, series and financial year. Only ever increases,
 * so a number is never reused. Allocated inside the same transaction that saves the invoice.
 * Note: the counter is per device; Phase 5 (sync) must decide how two phones avoid colliding.
 */
@Entity(
    tableName = "document_counter",
    indices = [Index(value = ["businessId", "series", "financialYear"], unique = true)],
)
data class DocumentCounterEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING,

    val series: String,
    val financialYear: String,
    val lastNumber: Int,
)
