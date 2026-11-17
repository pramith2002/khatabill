package com.teczo.khatabill.ui.billing

import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.GstScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InvoiceCalcTest {
    private fun business(scheme: GstScheme = GstScheme.REGULAR) = BusinessEntity(
        id = "b", businessId = "b", createdAt = 0, updatedAt = 0, name = "Shop", ownerName = "O", phone = "9845012345",
        address = "A", city = "Mysuru", state = "Karnataka", stateCode = "29", pincode = "570001", gstScheme = scheme,
    )

    private fun line(qty: String = "1", rate: String = "100", bp: Int = 1800, inclusive: Boolean = false,
                     discount: String = "", percent: Boolean = true) = LineState(
        key = "k", itemId = "i", name = "Agarbatti", hsn = null, unit = "kg", gstRateBp = bp, taxInclusive = inclusive,
        affectsStock = true, qty = qty, rate = rate, discountIsPercent = percent, discount = discount,
    )

    private fun form(vararg lines: LineState, place: String = "29", overall: String = "", charges: String = "") =
        InvoiceFormState(type = DocumentType.SALE, placeOfSupply = place, date = 0, lines = lines.toList(),
            overallDiscount = overall, otherCharges = charges)

    @Test fun noLinesIsEmpty() = assertEquals(CalcState.Empty, buildCalc(form(), business()))

    @Test fun simpleSaleMatchesTheCalculator() {
        val ok = buildCalc(form(line()), business()) as CalcState.Ok
        assertEquals(11_800, ok.result.grandTotal)
        assertEquals(listOf(1_000L), ok.qtys)
        assertEquals(listOf(10_000L), ok.rates)
    }

    @Test fun typedQuantityAndRateAreExact() {
        // 1.25 kg at 99.90 = 124.875 -> 124.88 (half up), 5% GST
        val ok = buildCalc(form(line(qty = "1.25", rate = "99.9", bp = 500)), business()) as CalcState.Ok
        assertEquals(12_488, ok.result.taxableTotal)
    }

    @Test fun badQuantityIsInvalid() {
        assertEquals(CalcState.Invalid, buildCalc(form(line(qty = "abc")), business()))
        assertEquals(CalcState.Invalid, buildCalc(form(line(qty = "0")), business()))
        assertEquals(CalcState.Invalid, buildCalc(form(line(qty = "")), business()))
    }

    @Test fun badRateIsInvalid() {
        assertEquals(CalcState.Invalid, buildCalc(form(line(rate = "")), business()))
        assertEquals(CalcState.Invalid, buildCalc(form(line(rate = "1.234")), business()))
    }

    @Test fun percentDiscountIsRememberedInBasisPoints() {
        val ok = buildCalc(form(line(discount = "10")), business()) as CalcState.Ok
        assertEquals(listOf(1_000), ok.discountPercentBp)
        assertEquals(9_000, ok.result.taxableTotal)
    }

    @Test fun amountDiscountHasNoPercent() {
        val ok = buildCalc(form(line(discount = "10", percent = false)), business()) as CalcState.Ok
        assertEquals(listOf(0), ok.discountPercentBp)
        assertEquals(9_000, ok.result.taxableTotal)
    }

    @Test fun discountLargerThanAmountIsInvalidNotACrash() {
        assertEquals(CalcState.Invalid, buildCalc(form(line(discount = "500", percent = false)), business()))
        assertEquals(CalcState.Invalid, buildCalc(form(line(discount = "101")), business()))
    }

    @Test fun overallDiscountAndExtraChargesAreApplied() {
        val ok = buildCalc(form(line(bp = 0), overall = "10", charges = "50"), business()) as CalcState.Ok
        assertEquals(9_000, ok.result.taxableTotal)
        assertEquals(5_000, ok.result.otherCharges)
        assertEquals(14_000, ok.result.grandTotal)
    }

    @Test fun otherStateGivesIgst() {
        val ok = buildCalc(form(line(), place = "27"), business()) as CalcState.Ok
        assertEquals(1_800, ok.result.igst)
        assertEquals(0, ok.result.cgst)
    }

    @Test fun compositionBusinessChargesNoTax() {
        val ok = buildCalc(form(line()), business(GstScheme.COMPOSITION)) as CalcState.Ok
        assertTrue(ok.result.billOfSupply)
        assertEquals(10_000, ok.result.grandTotal)
    }

    @Test fun badExtraChargesAreInvalid() =
        assertEquals(CalcState.Invalid, buildCalc(form(line(), charges = "x"), business()))
}
