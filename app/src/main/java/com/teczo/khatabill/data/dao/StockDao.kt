package com.teczo.khatabill.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.teczo.khatabill.data.entities.StockMovementEntity
import kotlinx.coroutines.flow.Flow

data class ItemStock(val itemId: String, val qty: Long)

@Dao
interface StockDao {
    @Insert
    suspend fun insert(movement: StockMovementEntity)

    @Insert
    suspend fun insertAll(movements: List<StockMovementEntity>)

    @Query("SELECT COALESCE(SUM(qtyDelta), 0) FROM stock_movement WHERE itemId = :itemId AND deletedAt IS NULL")
    suspend fun getStock(itemId: String): Long

    @Query("SELECT COALESCE(SUM(qtyDelta), 0) FROM stock_movement WHERE itemId = :itemId AND deletedAt IS NULL")
    fun observeStock(itemId: String): Flow<Long>

    @Query(
        """
        SELECT itemId, COALESCE(SUM(qtyDelta), 0) AS qty FROM stock_movement
        WHERE businessId = :businessId AND deletedAt IS NULL GROUP BY itemId
        """,
    )
    fun observeAllStock(businessId: String): Flow<List<ItemStock>>

    @Query("SELECT * FROM stock_movement WHERE itemId = :itemId AND deletedAt IS NULL ORDER BY date DESC, createdAt DESC")
    fun observeMovements(itemId: String): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movement WHERE refInvoiceId = :invoiceId AND deletedAt IS NULL")
    suspend fun getByInvoice(invoiceId: String): List<StockMovementEntity>
}
