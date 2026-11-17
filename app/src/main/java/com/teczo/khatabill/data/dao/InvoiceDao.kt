package com.teczo.khatabill.data.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.teczo.khatabill.data.entities.DocumentCounterEntity
import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.entities.InvoiceLineEntity
import com.teczo.khatabill.domain.model.DocumentType
import kotlinx.coroutines.flow.Flow

data class InvoiceWithLines(
    @Embedded val invoice: InvoiceEntity,
    @Relation(parentColumn = "id", entityColumn = "invoiceId") val lines: List<InvoiceLineEntity>,
)

/** There is intentionally no delete here: invoices are cancelled, never removed. */
@Dao
interface InvoiceDao {
    @Insert
    suspend fun insert(invoice: InvoiceEntity)

    @Insert
    suspend fun insertLines(lines: List<InvoiceLineEntity>)

    @Update
    suspend fun update(invoice: InvoiceEntity)

    @Query("SELECT * FROM invoice WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): InvoiceEntity?

    @Query("SELECT * FROM invoice_line WHERE invoiceId = :invoiceId AND deletedAt IS NULL ORDER BY lineNo")
    suspend fun getLines(invoiceId: String): List<InvoiceLineEntity>

    @Query(
        """
        SELECT * FROM invoice
        WHERE businessId = :businessId AND type = :type AND deletedAt IS NULL
        ORDER BY date DESC, sequenceNo DESC
        """,
    )
    fun observeByType(businessId: String, type: DocumentType): Flow<List<InvoiceEntity>>

    @Transaction
    @Query("SELECT * FROM invoice WHERE id = :id AND deletedAt IS NULL")
    fun observeWithLines(id: String): Flow<InvoiceWithLines?>

    @Query(
        """
        SELECT * FROM document_counter
        WHERE businessId = :businessId AND series = :series AND financialYear = :financialYear
        """,
    )
    suspend fun getCounter(businessId: String, series: String, financialYear: String): DocumentCounterEntity?

    @Insert
    suspend fun insertCounter(counter: DocumentCounterEntity)

    @Update
    suspend fun updateCounter(counter: DocumentCounterEntity)
}
