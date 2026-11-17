package com.teczo.khatabill.ui.billing

import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.domain.model.GstScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickBillTest {
    private fun business(scheme: GstScheme = GstScheme.REGULAR) = BusinessEntity(
        id = "b", businessId = "b", createdAt = 0, updatedAt = 0, name = "Shop", ownerName = "O", phone = "9845012345",
        address = "A", city = "Mysuru", state = "Karnataka", stateCode = "29", pincode = "570001", gstScheme = scheme,
    )

    @Test fun blankAmountIsInvalid() =
        assertEquals(CalcState.Invalid, buildQuickCalc(QuickBillForm(), business(), 0, "Quick sale"))

    @Test fun zeroAmountIsInvalid() =
        assertEquals(CalcState.Invalid, buildQuickCalc(QuickBillForm(amount = "0"), business(), 0, "Quick sale"))

    @Test fun amountIsWhatTheCustomerPaysWhenGstIsCharged() {
        val ok = buildQuickCalc(QuickBillForm(amount = "118", gstRateBp = 1800), business(), 0, "Quick sale") as CalcState.Ok
        assertEquals(11_800, ok.result.grandTotal)
        assertEquals(10_000, ok.result.taxableTotal)
        assertEquals(900, ok.result.cgst)
        assertEquals(900, ok.result.sgst)
    }

    @Test fun noTaxForCompositionBusiness() {
        val ok = buildQuickCalc(QuickBillForm(amount = "118", gstRateBp = 1800), business(GstScheme.COMPOSITION), 0, "Quick sale") as CalcState.Ok
        assertEquals(11_800, ok.result.grandTotal)
        assertEquals(0, ok.result.cgst + ok.result.sgst + ok.result.igst)
    }

    @Test fun blankDescriptionStillMakesABill() {
        assertTrue(buildQuickCalc(QuickBillForm(amount = "50"), business(), 0, "Quick sale") is CalcState.Ok)
    }
}
