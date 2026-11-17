package com.teczo.khatabill.ui.billing

import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.GstScheme
import com.teczo.khatabill.domain.model.PaymentMode

/** What is typed on the Quick bill screen: one amount, one GST rate, how it was paid. */
data class QuickBillForm(
    val amount: String = "",
    val description: String = "",
    val gstRateBp: Int = 0,
    val paymentMode: PaymentMode = PaymentMode.CASH,
)

/**
 * Turns a quick bill into the normal bill calculation: a single free-text line of quantity 1, where the amount
 * typed is what the customer pays (tax-inclusive when the business charges GST). Pure, so it can be unit tested.
 */
fun buildQuickCalc(form: QuickBillForm, business: BusinessEntity, date: Long, lineName: String): CalcState {
    val charges = business.gstScheme == GstScheme.REGULAR
    val line = LineState(
        key = "quick",
        itemId = null,
        name = form.description.trim().ifBlank { lineName },
        hsn = null,
        unit = "nos",
        gstRateBp = if (charges) form.gstRateBp else 0,
        taxInclusive = charges,
        affectsStock = false,
        qty = "1",
        rate = form.amount,
    )
    val calc = buildCalc(
        InvoiceFormState(type = DocumentType.SALE, placeOfSupply = business.stateCode, date = date, lines = listOf(line)),
        business,
    )
    // A zero amount is not a bill.
    return if (calc is CalcState.Ok && calc.result.grandTotal <= 0) CalcState.Invalid else calc
}
