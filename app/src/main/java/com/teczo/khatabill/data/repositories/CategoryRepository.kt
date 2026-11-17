package com.teczo.khatabill.data.repositories

import com.teczo.khatabill.data.dao.CategoryDao
import com.teczo.khatabill.data.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val dao: CategoryDao,
) {
    fun observeAll(businessId: String): Flow<List<CategoryEntity>> = dao.observeAll(businessId)

    suspend fun add(businessId: String, name: String): CategoryEntity {
        val now = System.currentTimeMillis()
        val category = CategoryEntity(
            id = UUID.randomUUID().toString(),
            businessId = businessId,
            createdAt = now,
            updatedAt = now,
            name = name.trim(),
        )
        dao.upsert(category)
        return category
    }
}
