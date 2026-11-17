package com.teczo.khatabill.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.teczo.khatabill.domain.model.SyncState

@Entity(tableName = "category", indices = [Index(value = ["businessId", "name"])])
data class CategoryEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING,

    val name: String,
    val nameKn: String? = null,
)
