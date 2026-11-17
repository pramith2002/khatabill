package com.teczo.khatabill.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.domain.model.SyncState

/**
 * Every value here is a snapshot taken at creation: editing a party or item later never changes it.
 * Invoices are never deleted; a wrong one is cancelled, so its number is never reused.
 * Amounts are paise.
 */
@Entity(
    tableName = "invoice",
    foreignKeys = [
        ForeignKey(
            entity = PartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["partyId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["businessId", "series", "financialYear", "sequenceNo"], unique = true),
        Index(value = ["businessId", "type", "date"]),
        Index(value = ["partyId"]),
    ],
)
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING,

    val type: DocumentType,
    val series: String,
    /** "2026-27" */
    val financialYear: String,
    val sequenceNo: Int,
    /** Display number, for example "INV/26-27/0001". */
    val number: String,
    /** Epoch day (days since 1970-01-01). */
    val date: Long,
    val dueDate: Long? = null,

    /** null for quick bills with no party. */
    val partyId: String? = null,
    val partyName: String? = null,
    val partyGstin: String? = null,
    val partyAddress: String? = null,
    val partyStateCode: String? = null,
    val placeOfSupplyStateCode: String,

    /** subtotal - discountTotal == taxableTotal. All on the taxable basis (see GstCalculator). */
    val subtotal: Long,
    val discountTotal: Long,
    val otherCharges: Long = 0,
    val taxableTotal: Long,
    val cgst: Long = 0,
    val sgst: Long = 0,
    val igst: Long = 0,
    val roundOff: Long = 0,
    val grandTotal: Long,
    /** Cached total of money received; in Phase 2 it is maintained from Payment rows, never typed in. */
    val paidAmount: Long = 0,
    val status: InvoiceStatus,
    val cancelledAt: Long? = null,
    val cancelReason: String? = null,

    val notes: String? = null,
    val terms: String? = null,
    val transportDetails: String? = null,
    val vehicleNo: String? = null,
)
