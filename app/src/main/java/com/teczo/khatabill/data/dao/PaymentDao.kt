package com.teczo.khatabill.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.teczo.khatabill.data.entities.PaymentAllocationEntity
import com.teczo.khatabill.data.entities.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Insert
    suspend fun insert(payment: PaymentEntity)

    @Update
    suspend fun update(payment: PaymentEntity)

    @Insert
    suspend fun insertAllocations(allocations: List<PaymentAllocationEntity>)

    @Update
    suspend fun updateAllocation(allocation: PaymentAllocationEntity)

    @Query("SELECT * FROM payment WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): PaymentEntity?

    @Query("SELECT * FROM payment WHERE invoiceId = :invoiceId AND deletedAt IS NULL")
    suspend fun getByInvoice(invoiceId: String): List<PaymentEntity>

    @Query("SELECT * FROM payment WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY date DESC, createdAt DESC")
    fun observeAll(businessId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payment WHERE partyId = :partyId AND deletedAt IS NULL ORDER BY date DESC, createdAt DESC")
    fun observeByParty(partyId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payment_allocation WHERE paymentId = :paymentId AND deletedAt IS NULL")
    suspend fun getAllocationsOfPayment(paymentId: String): List<PaymentAllocationEntity>

    @Query("SELECT * FROM payment_allocation WHERE invoiceId = :invoiceId AND deletedAt IS NULL")
    suspend fun getAllocationsOfInvoice(invoiceId: String): List<PaymentAllocationEntity>

    @Query("SELECT * FROM payment_allocation WHERE businessId = :businessId AND deletedAt IS NULL")
    fun observeAllocations(businessId: String): Flow<List<PaymentAllocationEntity>>
}
