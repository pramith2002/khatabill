package com.teczo.khatabill.data.repositories

import androidx.room.withTransaction
import com.teczo.khatabill.data.dao.ItemDao
import com.teczo.khatabill.data.dao.ItemStock
import com.teczo.khatabill.data.dao.StockDao
import com.teczo.khatabill.data.db.KhataBillDatabase
import com.teczo.khatabill.data.entities.ItemEntity
import com.teczo.khatabill.data.entities.StockMovementEntity
import com.teczo.khatabill.domain.model.StockReason
import com.teczo.khatabill.domain.model.SyncState
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val db: KhataBillDatabase,
    private val itemDao: ItemDao,
    private val stockDao: StockDao,
) {
    fun observe(businessId: String, query: String = ""): Flow<List<ItemEntity>> =
        itemDao.observe(businessId, query.trim())

    fun observeStock(itemId: String): Flow<Long> = stockDao.observeStock(itemId)

    fun observeAllStock(businessId: String): Flow<List<ItemStock>> = stockDao.observeAllStock(businessId)

    fun observeMovements(itemId: String) = stockDao.observeMovements(itemId)

    suspend fun get(id: String): ItemEntity? = itemDao.getById(id)

    fun observeById(id: String): Flow<ItemEntity?> = itemDao.observeById(id)

    suspend fun findByBarcode(businessId: String, barcode: String): ItemEntity? =
        itemDao.getByBarcode(businessId, barcode)

    /** Creates the item and, when [openingStock] > 0, its OPENING stock movement in one transaction. */
    suspend fun create(item: ItemEntity, openingStock: Long, date: Long) {
        require(openingStock >= 0) { "Opening stock cannot be negative" }
        db.withTransaction {
            itemDao.insert(item.copy(syncState = SyncState.PENDING))
            if (openingStock > 0) {
                stockDao.insert(movement(item, date, openingStock, StockReason.OPENING, note = null))
            }
        }
    }

    suspend fun update(item: ItemEntity) {
        itemDao.update(item.copy(updatedAt = System.currentTimeMillis(), syncState = SyncState.PENDING))
    }

    /** Manual stock adjustment; [qtyDelta] is positive or negative thousandths. */
    suspend fun adjustStock(item: ItemEntity, qtyDelta: Long, date: Long, note: String?) {
        require(qtyDelta != 0L) { "Adjustment cannot be zero" }
        stockDao.insert(movement(item, date, qtyDelta, StockReason.ADJUSTMENT, note))
    }

    private fun movement(item: ItemEntity, date: Long, delta: Long, reason: StockReason, note: String?): StockMovementEntity {
        val now = System.currentTimeMillis()
        return StockMovementEntity(
            id = UUID.randomUUID().toString(),
            businessId = item.businessId,
            createdAt = now,
            updatedAt = now,
            itemId = item.id,
            date = date,
            qtyDelta = delta,
            reason = reason,
            note = note,
        )
    }
}
