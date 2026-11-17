package com.teczo.khatabill.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.teczo.khatabill.data.entities.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Insert
    suspend fun insert(item: ItemEntity)

    @Update
    suspend fun update(item: ItemEntity)

    @Query("SELECT * FROM item WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): ItemEntity?

    @Query("SELECT * FROM item WHERE id = :id AND deletedAt IS NULL")
    fun observeById(id: String): Flow<ItemEntity?>

    @Query("SELECT * FROM item WHERE businessId = :businessId AND barcode = :barcode AND deletedAt IS NULL LIMIT 1")
    suspend fun getByBarcode(businessId: String, barcode: String): ItemEntity?

    /** Search matches the English name, the Kannada name, SKU and barcode. Empty query lists all. */
    @Query(
        """
        SELECT * FROM item
        WHERE businessId = :businessId AND deletedAt IS NULL
          AND (:query = '' OR name LIKE '%' || :query || '%'
               OR nameKn LIKE '%' || :query || '%'
               OR sku LIKE '%' || :query || '%'
               OR barcode = :query)
        ORDER BY name COLLATE NOCASE
        """,
    )
    fun observe(businessId: String, query: String): Flow<List<ItemEntity>>
}
