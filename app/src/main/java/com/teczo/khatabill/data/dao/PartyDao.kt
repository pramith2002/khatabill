package com.teczo.khatabill.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.teczo.khatabill.data.entities.PartyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartyDao {
    @Upsert
    suspend fun upsert(party: PartyEntity)

    @Query("SELECT * FROM party WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): PartyEntity?

    /** Search matches the English name, the Kannada name and the phone number. Empty query lists all. */
    @Query(
        """
        SELECT * FROM party
        WHERE businessId = :businessId AND deletedAt IS NULL
          AND (:query = '' OR name LIKE '%' || :query || '%'
               OR nameKn LIKE '%' || :query || '%'
               OR phone LIKE '%' || :query || '%')
        ORDER BY name COLLATE NOCASE
        """,
    )
    fun observe(businessId: String, query: String): Flow<List<PartyEntity>>
}
