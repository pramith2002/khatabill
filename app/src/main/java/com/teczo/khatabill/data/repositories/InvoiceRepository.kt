package com.teczo.khatabill.data.repositories

import androidx.room.withTransaction
import com.teczo.khatabill.data.dao.InvoiceDao
import com.teczo.khatabill.data.dao.InvoiceWithLines
import com.teczo.khatabill.data.dao.PaymentDao
import com.teczo.khatabill.data.dao.StockDao
import com.teczo.khatabill.data.entities.PaymentAllocationEntity
import com.teczo.khatabill.data.entities.PaymentEntity
import com.teczo.khatabill.domain.ledger.InvoiceStatusRules
import com.teczo.khatabill.domain.model.PaymentType
import com.teczo.khatabill.data.db.KhataBillDatabase
import com.teczo.khatabill.data.entities.DocumentCounterEntity
import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.entities.InvoiceLineEntity
import com.teczo.khatabill.data.entities.StockMovementEntity
import com.teczo.khatabill.domain.invoice.InvoiceNumber
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.domain.model.StockReason
import com.teczo.khatabill.domain.model.SyncState
import com.teczo.khatabill.util.dates.FinancialYear
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceRepository @Inject constructor(
    private val db: KhataBillDatabase,
    private val invoiceDao: InvoiceDao,
    private val stockDao: StockDao,
    private val paymentDao: PaymentDao,
) {
    fun observeByType(businessId: String, type: DocumentType): Flow<List<InvoiceEntity>> =
        invoiceDao.observeByType(businessId, type)

    fun observeWithLines(id: String): Flow<InvoiceWithLines?> = invoiceDao.observeWithLines(id)

    /** Every live invoice of the business, all types (balances, reports, dashboard). */
    fun observeAll(businessId: String): Flow<List<InvoiceEntity>> = invoiceDao.observeAll(businessId)

    fun observeByParty(partyId: String): Flow<List<InvoiceEntity>> = invoiceDao.observeByParty(partyId)

    fun observeLinesBetween(businessId: String, from: Long, to: Long): Flow<List<InvoiceLineEntity>> =
        invoiceDao.observeLinesBetween(businessId, from, to)

    /**
     * Saves an invoice, its lines, the number counter and the stock movements in ONE transaction:
     * either everything is written or nothing is. A number is never handed out twice.
     */
    suspend fun createInvoice(draft: InvoiceDraft): InvoiceEntity {
        validate(draft)
        return db.withTransaction {
            val now = System.currentTimeMillis()
            val financialYear = FinancialYear.label(draft.date, draft.financialYearStartMonth)
            val sequenceNo = nextSequence(draft, financialYear, now)

            val invoice = InvoiceEntity(
                id = UUID.randomUUID().toString(),
                businessId = draft.businessId,
                createdAt = now,
                updatedAt = now,
                syncState = SyncState.PENDING,
                type = draft.type,
                series = draft.series,
                financialYear = financialYear,
                sequenceNo = sequenceNo,
                number = InvoiceNumber.format(draft.prefix, draft.series, financialYear, sequenceNo),
                date = draft.date,
                dueDate = draft.dueDate,
                partyId = draft.partyId,
                partyName = draft.partyName,
                partyGstin = draft.partyGstin,
                partyAddress = draft.partyAddress,
                partyStateCode = draft.partyStateCode,
                placeOfSupplyStateCode = draft.placeOfSupplyStateCode,
                subtotal = draft.subtotal,
                discountTotal = draft.discountTotal,
                otherCharges = draft.otherCharges,
                taxableTotal = draft.taxableTotal,
                cgst = draft.cgst,
                sgst = draft.sgst,
                igst = draft.igst,
                roundOff = draft.roundOff,
                grandTotal = draft.grandTotal,
                paidAmount = draft.paidAmount,
                status = InvoiceStatusRules.statusFor(draft.grandTotal, draft.paidAmount),
                notes = draft.notes,
                terms = draft.terms,
                transportDetails = draft.transportDetails,
                vehicleNo = draft.vehicleNo,
            )
            invoiceDao.insert(invoice)
            invoiceDao.insertLines(
                draft.lines.mapIndexed { i, l ->
                    InvoiceLineEntity(
                        id = UUID.randomUUID().toString(),
                        businessId = draft.businessId,
                        createdAt = now,
                        updatedAt = now,
                        invoiceId = invoice.id,
                        lineNo = i + 1,
                        itemId = l.itemId,
                        nameSnapshot = l.nameSnapshot,
                        hsnSac = l.hsnSac,
                        qty = l.qty,
                        unit = l.unit,
                        rate = l.rate,
                        taxInclusive = l.taxInclusive,
                        discountPercentBp = l.discountPercentBp,
                        discountAmount = l.discountAmount,
                        taxableValue = l.taxableValue,
                        gstRateBp = l.gstRateBp,
                        cgst = l.cgst,
                        sgst = l.sgst,
                        igst = l.igst,
                        lineTotal = l.lineTotal,
                    )
                },
            )

            if (draft.paidAmount > 0) {
                // Money taken at billing time: a payment tied to this invoice, fully allocated to it.
                val payment = PaymentEntity(
                    id = UUID.randomUUID().toString(), businessId = draft.businessId, createdAt = now, updatedAt = now,
                    type = if (draft.type == DocumentType.PURCHASE) PaymentType.PAID else PaymentType.RECEIVED,
                    partyId = draft.partyId, invoiceId = invoice.id, date = draft.date,
                    amount = draft.paidAmount, mode = draft.paymentMode,
                )
                paymentDao.insert(payment)
                paymentDao.insertAllocations(
                    listOf(
                        PaymentAllocationEntity(
                            id = UUID.randomUUID().toString(), businessId = draft.businessId, createdAt = now, updatedAt = now,
                            paymentId = payment.id, invoiceId = invoice.id, amount = draft.paidAmount,
                        ),
                    ),
                )
            }

            val sign = stockSign(draft.type)
            if (sign != 0L) {
                val reason = stockReason(draft.type)
                val movements = draft.lines.filter { it.affectsStock && it.itemId != null }.map { l ->
                    StockMovementEntity(
                        id = UUID.randomUUID().toString(),
                        businessId = draft.businessId,
                        createdAt = now,
                        updatedAt = now,
                        itemId = l.itemId!!,
                        date = draft.date,
                        qtyDelta = sign * l.qty,
                        reason = reason,
                        refInvoiceId = invoice.id,
                    )
                }
                if (movements.isNotEmpty()) stockDao.insertAll(movements)
            }
            invoice
        }
    }

    /**
     * Cancels an invoice (it is never deleted, so its number stays used) and puts its stock back
     * with reversing movements, all in one transaction.
     */
    suspend fun cancelInvoice(id: String, reason: String) {
        require(reason.isNotBlank()) { "A cancel reason is required" }
        db.withTransaction {
            val invoice = invoiceDao.getById(id) ?: error("Invoice not found")
            require(invoice.status != InvoiceStatus.CANCELLED) { "Invoice is already cancelled" }
            val now = System.currentTimeMillis()
            invoiceDao.update(
                invoice.copy(
                    status = InvoiceStatus.CANCELLED,
                    cancelledAt = now,
                    cancelReason = reason.trim(),
                    updatedAt = now,
                    syncState = SyncState.PENDING,
                ),
            )
            // Money taken at billing time is refunded with the invoice; money paid later stays on account.
            for (payment in paymentDao.getByInvoice(id)) {
                paymentDao.update(payment.copy(deletedAt = now, updatedAt = now, syncState = SyncState.PENDING))
            }
            for (alloc in paymentDao.getAllocationsOfInvoice(id)) {
                paymentDao.updateAllocation(alloc.copy(deletedAt = now, updatedAt = now, syncState = SyncState.PENDING))
            }
            val reversals = stockDao.getByInvoice(id).map { m ->
                m.copy(
                    id = UUID.randomUUID().toString(),
                    createdAt = now,
                    updatedAt = now,
                    qtyDelta = -m.qtyDelta,
                    note = "cancelled ${invoice.number}",
                )
            }
            if (reversals.isNotEmpty()) stockDao.insertAll(reversals)
        }
    }

    private suspend fun nextSequence(draft: InvoiceDraft, financialYear: String, now: Long): Int {
        val existing = invoiceDao.getCounter(draft.businessId, draft.series, financialYear)
        val next = (existing?.lastNumber ?: 0) + 1
        if (existing == null) {
            invoiceDao.insertCounter(
                DocumentCounterEntity(
                    id = UUID.randomUUID().toString(),
                    businessId = draft.businessId,
                    createdAt = now,
                    updatedAt = now,
                    series = draft.series,
                    financialYear = financialYear,
                    lastNumber = next,
                ),
            )
        } else {
            invoiceDao.updateCounter(
                existing.copy(lastNumber = next, updatedAt = now, syncState = SyncState.PENDING),
            )
        }
        return next
    }

    /** Refuses to save numbers that do not add up (a bug upstream must never reach the database). */
    private fun validate(d: InvoiceDraft) {
        require(d.lines.isNotEmpty()) { "An invoice needs at least one line" }
        require(d.prefix.length <= 2) { "Invoice prefix can be at most 2 characters" }
        require(d.lines.sumOf { it.taxableValue } == d.taxableTotal) { "Line taxable values do not add up" }
        require(d.lines.sumOf { it.cgst } == d.cgst) { "Line CGST does not add up" }
        require(d.lines.sumOf { it.sgst } == d.sgst) { "Line SGST does not add up" }
        require(d.lines.sumOf { it.igst } == d.igst) { "Line IGST does not add up" }
        require(d.subtotal - d.discountTotal == d.taxableTotal) { "Subtotal minus discount must equal taxable total" }
        require(d.taxableTotal + d.cgst + d.sgst + d.igst + d.otherCharges + d.roundOff == d.grandTotal) {
            "Grand total does not add up"
        }
        require(d.paidAmount in 0..d.grandTotal) { "Paid amount must be between zero and the grand total" }
        require(d.paidAmount == 0L || d.type == DocumentType.SALE || d.type == DocumentType.PURCHASE) {
            "Only sales and purchases can be paid at billing time"
        }
    }

    /** +1 stock in, -1 stock out, 0 no stock effect (quotations, proformas, challans). */
    private fun stockSign(type: DocumentType): Long = when (type) {
        DocumentType.SALE, DocumentType.PURCHASE_RETURN -> -1
        DocumentType.PURCHASE, DocumentType.SALE_RETURN -> 1
        DocumentType.QUOTATION, DocumentType.PROFORMA, DocumentType.DELIVERY_CHALLAN -> 0
    }

    private fun stockReason(type: DocumentType): StockReason = when (type) {
        DocumentType.SALE -> StockReason.SALE
        DocumentType.PURCHASE -> StockReason.PURCHASE
        else -> StockReason.RETURN
    }
}
