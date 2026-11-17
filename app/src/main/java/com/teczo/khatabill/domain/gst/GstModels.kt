package com.teczo.khatabill.domain.gst

import com.teczo.khatabill.domain.model.GstScheme

/**
 * Units used everywhere in the GST code:
 *  - money: Long paise
 *  - quantity: Long thousandths (1.250 kg = 1250)
 *  - tax rates and percentage discounts: Int basis points (18% = 1800)
 */

/** How an item is taxed. EXEMPT and ZERO_RATED both charge no tax but are reported separately later. */
enum class TaxTreatment { TAXABLE, EXEMPT, ZERO_RATED }

sealed interface Discount {
    data object None : Discount
    data class Percent(val basisPoints: Int) : Discount
    data class Amount(val paise: Long) : Discount
}

data class GstLineInput(
    val qty: Long,
    /** Price per unit in paise. Includes tax when [taxInclusive] is true. */
    val rate: Long,
    val taxInclusive: Boolean,
    val gstRateBp: Int,
    val treatment: TaxTreatment = TaxTreatment.TAXABLE,
    val discount: Discount = Discount.None,
)

data class GstInvoiceInput(
    val lines: List<GstLineInput>,
    val businessStateCode: String,
    /** Blank means "same state as the business" (typical for a walk-in customer). */
    val placeOfSupplyStateCode: String,
    val scheme: GstScheme,
    val overallDiscount: Discount = Discount.None,
    /** Extra charges (freight, packing). Added after tax; no GST is calculated on them in v1. */
    val otherCharges: Long = 0,
    val roundToRupee: Boolean = true,
)

enum class SupplyType {
    /** CGST + SGST */
    INTRA_STATE,

    /** IGST */
    INTER_STATE,

    /** No tax is charged (composition or unregistered business: bill of supply). */
    NONE,
}

data class GstLineResult(
    /** Quantity times rate, exactly as entered (includes tax for tax-inclusive lines). */
    val grossAmount: Long,
    /** Taxable value before any discount. subtotal - discountAmount == taxableValue, always. */
    val subtotal: Long,
    /** Line discount plus this line's share of the overall discount, on the taxable basis. */
    val discountAmount: Long,
    val taxableValue: Long,
    val gstRateBp: Int,
    val treatment: TaxTreatment,
    val cgst: Long,
    val sgst: Long,
    val igst: Long,
    /** taxableValue + cgst + sgst + igst */
    val lineTotal: Long,
)

data class GstInvoiceResult(
    val lines: List<GstLineResult>,
    val supply: SupplyType,
    val billOfSupply: Boolean,
    val subtotal: Long,
    val discountTotal: Long,
    val taxableTotal: Long,
    val cgst: Long,
    val sgst: Long,
    val igst: Long,
    val otherCharges: Long,
    val roundOff: Long,
    val grandTotal: Long,
)
