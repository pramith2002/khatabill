package com.teczo.khatabill.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.teczo.khatabill.data.entities.BusinessEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Upsert
    suspend fun upsert(business: BusinessEntity)

    /** Phase 1 has a single business; the multi-business switcher comes later. */
    @Query("SELECT * FROM business WHERE deletedAt IS NULL ORDER BY createdAt LIMIT 1")
    fun observeFirst(): Flow<BusinessEntity?>

    @Query("SELECT * FROM business WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): BusinessEntity?
}
