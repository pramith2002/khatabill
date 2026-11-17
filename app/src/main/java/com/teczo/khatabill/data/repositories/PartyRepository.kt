package com.teczo.khatabill.data.repositories

import com.teczo.khatabill.data.dao.PartyDao
import com.teczo.khatabill.data.entities.PartyEntity
import com.teczo.khatabill.domain.model.SyncState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartyRepository @Inject constructor(
    private val dao: PartyDao,
) {
    fun observe(businessId: String, query: String = ""): Flow<List<PartyEntity>> =
        dao.observe(businessId, query.trim())

    suspend fun get(id: String): PartyEntity? = dao.getById(id)

    suspend fun save(party: PartyEntity) {
        dao.upsert(party.copy(updatedAt = System.currentTimeMillis(), syncState = SyncState.PENDING))
    }

    /** Soft delete: old invoices keep their party snapshot and stay valid. */
    suspend fun delete(id: String) {
        val party = dao.getById(id) ?: return
        val now = System.currentTimeMillis()
        dao.upsert(party.copy(deletedAt = now, updatedAt = now, syncState = SyncState.PENDING))
    }
}
