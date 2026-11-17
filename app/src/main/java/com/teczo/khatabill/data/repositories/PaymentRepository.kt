package com.teczo.khatabill.data.repositories

import androidx.room.withTransaction
import com.teczo.khatabill.data.dao.InvoiceDao
import com.teczo.khatabill.data.dao.PaymentDao
import com.teczo.khatabill.data.db.KhataBillDatabase
import com.teczo.khatabill.data.entities.PaymentAllocationEntity
import com.teczo.khatabill.data.entities.PaymentEntity
import com.teczo.khatabill.domain.ledger.AllocationPlanner
import com.teczo.khatabill.domain.ledger.InvoiceStatusRules
import com.teczo.khatabill.domain.ledger.OpenInvoice
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.domain.model.PaymentMode
import com.teczo.khatabill.domain.model.PaymentType
import com.teczo.khatabill.domain.model.SyncState
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val db: KhataBillDatabase,
    private val paymentDao: PaymentDao,
    private val invoiceDao: InvoiceDao,
) {
    fun observeAll(businessId: String): Flow<List<PaymentEntity>> = paymentDao.observeAll(businessId)

    fun observeByParty(partyId: String): Flow<List<PaymentEntity>> = paymentDao.observeByParty(partyId)

    fun observeAllocations(businessId: String) = paymentDao.observeAllocations(businessId)

    /**
     * Records money received from (or paid to) a party and allocates it to their open invoices, oldest
     * first (or [preferredInvoiceId] first). Whatever is left stays on account. Everything is one transaction,
     * so an invoice's paid amount and the ledger can never disagree.
     */
    suspend fun record(
        businessId: String,
        type: PaymentType,
        partyId: String,
        date: Long,
        amount: Long,
        mode: PaymentMode,
        reference: String?,
        notes: String?,
        preferredInvoiceId: String? = null,
    ): PaymentEntity {
        require(amount > 0) { "Payment must be positive" }
        return db.withTransaction {
            val now = System.currentTimeMillis()
            val payment = PaymentEntity(
                id = UUID.randomUUID().toString(), businessId = businessId, createdAt = now, updatedAt = now,
                type = type, partyId = partyId, date = date, amount = amount, mode = mode,
                reference = reference?.trim()?.ifBlank { null }, notes = notes?.trim()?.ifBlank { null },
            )
            paymentDao.insert(payment)

            val docType = if (type == PaymentType.RECEIVED) DocumentType.SALE else DocumentType.PURCHASE
            val open = invoiceDao.getOpenInvoices(businessId, partyId, docType)
            val plan = AllocationPlanner.plan(
                amount, open.map { OpenInvoice(it.id, it.grandTotal - it.paidAmount) }, preferredInvoiceId,
            )
            val byId = open.associateBy { it.id }
            val rows = plan.allocations.map { a ->
                PaymentAllocationEntity(
                    id = UUID.randomUUID().toString(), businessId = businessId, createdAt = now, updatedAt = now,
                    paymentId = payment.id, invoiceId = a.invoiceId, amount = a.amount,
                )
            }
            if (rows.isNotEmpty()) paymentDao.insertAllocations(rows)
            for (a in plan.allocations) {
                val inv = byId.getValue(a.invoiceId)
                val paid = inv.paidAmount + a.amount
                invoiceDao.update(
                    inv.copy(paidAmount = paid, status = InvoiceStatusRules.statusFor(inv.grandTotal, paid), updatedAt = now, syncState = SyncState.PENDING),
                )
            }
            payment
        }
    }

    /**
     * "Mark as paid": money against ONE invoice (for example a walk-in UPI bill that has no party).
     * At most the invoice's remaining balance is applied. Returns the amount that was applied.
     */
    suspend fun recordAgainstInvoice(invoiceId: String, amount: Long, mode: PaymentMode, date: Long, reference: String? = null): Long {
        require(amount > 0) { "Payment must be positive" }
        return db.withTransaction {
            val inv = invoiceDao.getById(invoiceId) ?: error("Invoice not found")
            require(inv.status != InvoiceStatus.CANCELLED) { "Invoice is cancelled" }
            val applied = minOf(amount, inv.grandTotal - inv.paidAmount)
            require(applied > 0) { "Invoice is already paid" }
            val now = System.currentTimeMillis()
            val payment = PaymentEntity(
                id = UUID.randomUUID().toString(), businessId = inv.businessId, createdAt = now, updatedAt = now,
                type = if (inv.type == DocumentType.PURCHASE) PaymentType.PAID else PaymentType.RECEIVED,
                partyId = inv.partyId, date = date, amount = applied, mode = mode, reference = reference?.trim()?.ifBlank { null },
            )
            paymentDao.insert(payment)
            paymentDao.insertAllocations(
                listOf(
                    PaymentAllocationEntity(
                        id = UUID.randomUUID().toString(), businessId = inv.businessId, createdAt = now, updatedAt = now,
                        paymentId = payment.id, invoiceId = inv.id, amount = applied,
                    ),
                ),
            )
            val paid = inv.paidAmount + applied
            invoiceDao.update(
                inv.copy(paidAmount = paid, status = InvoiceStatusRules.statusFor(inv.grandTotal, paid), updatedAt = now, syncState = SyncState.PENDING),
            )
            applied
        }
    }

    /** Soft-deletes a payment and takes its money back off the invoices it was allocated to. */
    suspend fun delete(paymentId: String) {
        db.withTransaction {
            val payment = paymentDao.getById(paymentId) ?: return@withTransaction
            val now = System.currentTimeMillis()
            for (alloc in paymentDao.getAllocationsOfPayment(paymentId)) {
                val inv = invoiceDao.getById(alloc.invoiceId)
                if (inv != null && inv.status != InvoiceStatus.CANCELLED) {
                    val paid = (inv.paidAmount - alloc.amount).coerceAtLeast(0)
                    invoiceDao.update(
                        inv.copy(paidAmount = paid, status = InvoiceStatusRules.statusFor(inv.grandTotal, paid), updatedAt = now, syncState = SyncState.PENDING),
                    )
                }
                paymentDao.updateAllocation(alloc.copy(deletedAt = now, updatedAt = now, syncState = SyncState.PENDING))
            }
            paymentDao.update(payment.copy(deletedAt = now, updatedAt = now, syncState = SyncState.PENDING))
        }
    }
}
