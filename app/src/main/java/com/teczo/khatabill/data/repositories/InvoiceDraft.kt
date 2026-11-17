package com.teczo.khatabill.data.repositories

import com.teczo.khatabill.domain.model.DocumentType

/**
 * What the billing screen hands to [InvoiceRepository.createInvoice]. All amounts are already
 * calculated by GstCalculator; the repository only checks that they add up before saving.
 */
data class InvoiceDraft(
    val businessId: String,
    val type: DocumentType,
    val series: String,
    /** Business invoice prefix, at most 2 characters. */
    val prefix: String,
    val financialYearStartMonth: Int,
    val date: Long,
    val dueDate: Long?,
    val partyId: String?,
    val partyName: String?,
    val partyGstin: String?,
    val partyAddress: String?,
    val partyStateCode: String?,
    val placeOfSupplyStateCode: String,
    val subtotal: Long,
    val discountTotal: Long,
    val otherCharges: Long,
    val taxableTotal: Long,
    val cgst: Long,
    val sgst: Long,
    val igst: Long,
    val roundOff: Long,
    val grandTotal: Long,
    val paidAmount: Long,
    val notes: String?,
    val terms: String?,
    val transportDetails: String?,
    val vehicleNo: String?,
    val lines: List<InvoiceLineDraft>,
)

data class InvoiceLineDraft(
    val itemId: String?,
    val nameSnapshot: String,
    val hsnSac: String?,
    val qty: Long,
    val unit: String,
    val rate: Long,
    val taxInclusive: Boolean,
    val discountPercentBp: Int,
    val discountAmount: Long,
    val taxableValue: Long,
    val gstRateBp: Int,
    val cgst: Long,
    val sgst: Long,
    val igst: Long,
    val lineTotal: Long,
    /** false for services and free-text lines: no stock movement is written. */
    val affectsStock: Boolean,
)
