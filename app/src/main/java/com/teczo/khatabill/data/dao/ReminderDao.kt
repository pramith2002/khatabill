package com.teczo.khatabill.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.teczo.khatabill.data.entities.ReminderEntity
import com.teczo.khatabill.domain.model.ReminderChannel
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert
    suspend fun insert(reminder: ReminderEntity)

    @Query("SELECT * FROM reminder WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY scheduledAt DESC")
    fun observeAll(businessId: String): Flow<List<ReminderEntity>>

    /** Used by the daily worker so the same invoice is not notified twice in one day. */
    @Query(
        """
        SELECT COUNT(*) FROM reminder
        WHERE invoiceId = :invoiceId AND channel = :channel AND scheduledAt >= :since AND deletedAt IS NULL
        """,
    )
    suspend fun countSince(invoiceId: String, channel: ReminderChannel, since: Long): Int
}
