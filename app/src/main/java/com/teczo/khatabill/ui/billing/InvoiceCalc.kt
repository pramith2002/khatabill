package com.teczo.khatabill.ui.billing

import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.data.entities.PartyEntity
import com.teczo.khatabill.domain.gst.Discount
import com.teczo.khatabill.domain.gst.GstCalculator
import com.teczo.khatabill.domain.gst.GstInvoiceInput
import com.teczo.khatabill.domain.gst.GstInvoiceResult
import com.teczo.khatabill.domain.gst.GstLineInput
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.util.money.MoneyInput

/** One line as typed on the screen. Text fields stay text until they are parsed for the calculation. */
data class LineState(
    val key: String,
    val itemId: String?,
    val name: String,
    val hsn: String?,
    val unit: String,
    val gstRateBp: Int,
    val taxInclusive: Boolean,
    val affectsStock: Boolean,
    val qty: String,
    val rate: String,
    val discountIsPercent: Boolean = true,
    val discount: String = "",
)

data class InvoiceFormState(
    val type: DocumentType,
    val party: PartyEntity? = null,
    /** GST state code. For a purchase this is the supplier's state. */
    val placeOfSupply: String = "",
    val date: Long,
    val lines: List<LineState> = emptyList(),
    val overallIsPercent: Boolean = true,
    val overallDiscount: String = "",
    val otherCharges: String = "",
    val paid: String = "",
    val notes: String = "",
)

sealed interface CalcState {
    /** No items yet. */
    data object Empty : CalcState

    /** Something typed is not a valid number, or a discount is larger than the amount. */
    data object Invalid : CalcState

    data class Ok(
        val result: GstInvoiceResult,
        val qtys: List<Long>,
        val rates: List<Long>,
        val discountPercentBp: List<Int>,
    ) : CalcState
}

/** Turns the typed form into a GstCalculator result. Pure, so it can be unit tested. */
fun buildCalc(form: InvoiceFormState, business: BusinessEntity): CalcState {
    if (form.lines.isEmpty()) return CalcState.Empty
    val inputs = ArrayList<GstLineInput>()
    val qtys = ArrayList<Long>()
    val rates = ArrayList<Long>()
    val percents = ArrayList<Int>()
    for (line in form.lines) {
        val qty = MoneyInput.parseQty(line.qty)?.takeIf { it > 0 } ?: return CalcState.Invalid
        val rate = MoneyInput.parsePaise(line.rate) ?: return CalcState.Invalid
        val discount = parseDiscount(line.discount, line.discountIsPercent) ?: return CalcState.Invalid
        inputs += GstLineInput(qty, rate, line.taxInclusive, line.gstRateBp, discount = discount)
        qtys += qty
        rates += rate
        percents += (discount as? Discount.Percent)?.basisPoints ?: 0
    }
    val overall = parseDiscount(form.overallDiscount, form.overallIsPercent) ?: return CalcState.Invalid
    val charges = if (form.otherCharges.isBlank()) 0L else MoneyInput.parsePaise(form.otherCharges) ?: return CalcState.Invalid
    return try {
        val result = GstCalculator.calculate(
            GstInvoiceInput(
                lines = inputs,
                businessStateCode = business.stateCode,
                placeOfSupplyStateCode = form.placeOfSupply,
                scheme = business.gstScheme,
                overallDiscount = overall,
                otherCharges = charges,
            ),
        )
        CalcState.Ok(result, qtys, rates, percents)
    } catch (e: IllegalArgumentException) {
        CalcState.Invalid
    }
}

private fun parseDiscount(text: String, percent: Boolean): Discount? = when {
    text.isBlank() -> Discount.None
    percent -> MoneyInput.parsePercentBp(text)?.let { Discount.Percent(it) }
    else -> MoneyInput.parsePaise(text)?.let { Discount.Amount(it) }
}
