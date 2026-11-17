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

    /** Every live invoice of the business, all types (used for balances and reports). */
    @Query("SELECT * FROM invoice WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY date DESC, sequenceNo DESC")
    fun observeAll(businessId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoice WHERE partyId = :partyId AND deletedAt IS NULL ORDER BY date DESC, sequenceNo DESC")
    fun observeByParty(partyId: String): Flow<List<InvoiceEntity>>

    /** Lines of every live bill dated in the range (for item-wise, HSN and B2C reports). Cancelled bills are excluded. */
    @Query(
        """
        SELECT l.* FROM invoice_line l JOIN invoice i ON l.invoiceId = i.id
        WHERE i.businessId = :businessId AND i.date BETWEEN :from AND :to AND i.status NOT IN ('CANCELLED', 'DRAFT')
          AND i.deletedAt IS NULL AND l.deletedAt IS NULL
        """,
    )
    fun observeLinesBetween(businessId: String, from: Long, to: Long): Flow<List<InvoiceLineEntity>>

    /** Unpaid or part-paid invoices of a party, oldest first: the order payments are allocated in. */
    @Query(
        """
        SELECT * FROM invoice
        WHERE businessId = :businessId AND partyId = :partyId AND type = :type
          AND status IN ('UNPAID', 'PARTIAL') AND deletedAt IS NULL
        ORDER BY date, sequenceNo
        """,
    )
    suspend fun getOpenInvoices(businessId: String, partyId: String, type: DocumentType): List<InvoiceEntity>

    /** Unpaid sales whose due date has arrived (for due-date notifications). */
    @Query(
        """
        SELECT * FROM invoice
        WHERE businessId = :businessId AND type = 'SALE' AND status IN ('UNPAID', 'PARTIAL')
          AND dueDate IS NOT NULL AND dueDate <= :day AND deletedAt IS NULL
        ORDER BY dueDate
        """,
    )
    suspend fun getDueUpTo(businessId: String, day: Long): List<InvoiceEntity>

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
