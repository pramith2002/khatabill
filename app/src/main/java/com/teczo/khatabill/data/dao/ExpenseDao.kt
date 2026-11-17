package com.teczo.khatabill.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.teczo.khatabill.data.entities.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Upsert
    suspend fun upsert(expense: ExpenseEntity)

    @Query("SELECT * FROM expense WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): ExpenseEntity?

    @Query("SELECT * FROM expense WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY date DESC, createdAt DESC")
    fun observeAll(businessId: String): Flow<List<ExpenseEntity>>
}
