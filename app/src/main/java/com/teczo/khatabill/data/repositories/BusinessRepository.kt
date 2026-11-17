package com.teczo.khatabill.data.repositories

import com.teczo.khatabill.data.dao.BusinessDao
import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.domain.model.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusinessRepository @Inject constructor(
    private val dao: BusinessDao,
) {
    /** The current business, or null before setup is finished. */
    fun observeActive(): Flow<BusinessEntity?> = dao.observeFirst()

    /** Only emits once a business exists; use inside screens that are reachable only after setup. */
    val active: Flow<BusinessEntity> get() = dao.observeFirst().filterNotNull()

    /** Insert or update. Marks the row PENDING so sync (Phase 5) picks it up. */
    suspend fun save(business: BusinessEntity) {
        dao.upsert(business.copy(updatedAt = System.currentTimeMillis(), syncState = SyncState.PENDING))
    }
}
