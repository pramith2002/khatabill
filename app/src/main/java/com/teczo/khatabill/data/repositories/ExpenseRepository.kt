package com.teczo.khatabill.data.repositories

import com.teczo.khatabill.data.dao.ExpenseDao
import com.teczo.khatabill.data.entities.ExpenseEntity
import com.teczo.khatabill.domain.model.SyncState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val dao: ExpenseDao,
) {
    fun observeAll(businessId: String): Flow<List<ExpenseEntity>> = dao.observeAll(businessId)

    suspend fun save(expense: ExpenseEntity) {
        dao.upsert(expense.copy(updatedAt = System.currentTimeMillis(), syncState = SyncState.PENDING))
    }

    /** Soft delete, so synced devices learn about it. */
    suspend fun delete(id: String) {
        val e = dao.getById(id) ?: return
        val now = System.currentTimeMillis()
        dao.upsert(e.copy(deletedAt = now, updatedAt = now, syncState = SyncState.PENDING))
    }
}
