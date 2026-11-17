package com.teczo.khatabill.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.teczo.khatabill.data.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Query("SELECT * FROM category WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY name COLLATE NOCASE")
    fun observeAll(businessId: String): Flow<List<CategoryEntity>>
}
