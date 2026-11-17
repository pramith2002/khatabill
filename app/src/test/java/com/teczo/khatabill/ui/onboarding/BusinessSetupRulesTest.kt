package com.teczo.khatabill.ui.onboarding

import com.teczo.khatabill.domain.model.GstScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessSetupRulesTest {

    private val goodShop = SetupForm(
        shopName = "Sri Ganesh Agarbatti", ownerName = "Ramesh", mobile = "9845012345",
        address = "Devaraja Mohalla", city = "Mysuru", pincode = "570001",
    )

    @Test fun completeShopStepHasNoErrors() = assertTrue(BusinessSetupRules.validateShop(goodShop).isEmpty())

    @Test fun emptyShopStepRequiresEveryField() {
        val errors = BusinessSetupRules.validateShop(SetupForm())
        assertEquals(
            setOf(SetupField.SHOP_NAME, SetupField.OWNER_NAME, SetupField.MOBILE, SetupField.ADDRESS, SetupField.CITY, SetupField.PINCODE),
            errors.keys,
        )
        assertTrue(errors.values.all { it == FieldError.REQUIRED })
    }

    @Test fun badMobileAndPincode() {
        val errors = BusinessSetupRules.validateShop(goodShop.copy(mobile = "12345", pincode = "5700"))
        assertEquals(FieldError.MOBILE, errors[SetupField.MOBILE])
        assertEquals(FieldError.PINCODE, errors[SetupField.PINCODE])
    }

    @Test fun noGstOnlyNeedsAState() {
        val f = SetupForm(gstScheme = GstScheme.UNREGISTERED)
        assertEquals(mapOf(SetupField.STATE to FieldError.REQUIRED), BusinessSetupRules.validateGst(f))
        assertTrue(BusinessSetupRules.validateGst(f.copy(stateCode = "29")).isEmpty())
    }

    @Test fun regularSchemeNeedsAGstin() {
        val errors = BusinessSetupRules.validateGst(SetupForm(gstScheme = GstScheme.REGULAR, stateCode = "29"))
        assertEquals(FieldError.REQUIRED, errors[SetupField.GSTIN])
    }

    @Test fun compositionSchemeAlsoNeedsAGstin() {
        val errors = BusinessSetupRules.validateGst(SetupForm(gstScheme = GstScheme.COMPOSITION, stateCode = "29"))
        assertEquals(FieldError.REQUIRED, errors[SetupField.GSTIN])
    }

    @Test fun gstinProblemsAreReportedSpecifically() {
        fun err(g: String) = BusinessSetupRules.validateGst(
            SetupForm(gstScheme = GstScheme.REGULAR, gstin = g, stateCode = "29"),
        )[SetupField.GSTIN]
        assertEquals(FieldError.GSTIN_LENGTH, err("29AAGCB7383J1Z"))
        assertEquals(FieldError.GSTIN_FORMAT, err("29AAGCB7383J1X4"))
        assertEquals(FieldError.GSTIN_STATE, err("39AAGCB7383J1Z4"))
        assertEquals(FieldError.GSTIN_CHECKSUM, err("29AAGCB7383J1Z5"))
    }

    @Test fun validGstinFillsStateAndPan() {
        val f = BusinessSetupRules.applyGstin(SetupForm(gstScheme = GstScheme.REGULAR, gstin = "29AAGCB7383J1Z4"))
        assertEquals("29", f.stateCode)
        assertEquals("AAGCB7383J", f.pan)
        assertTrue(BusinessSetupRules.validateGst(f).isEmpty())
    }

    @Test fun invalidGstinLeavesStateAlone() {
        val f = BusinessSetupRules.applyGstin(SetupForm(gstin = "29AAGCB7383J1Z5", stateCode = "27"))
        assertEquals("27", f.stateCode)
    }

    @Test fun optionalPanIsCheckedWhenGiven() {
        val f = SetupForm(gstScheme = GstScheme.UNREGISTERED, stateCode = "29", pan = "BAD")
        assertEquals(FieldError.PAN, BusinessSetupRules.validateGst(f)[SetupField.PAN])
    }

    @Test fun paymentsStepIsAllOptionalButChecked() {
        assertTrue(BusinessSetupRules.validatePayments(SetupForm()).isEmpty())
        val errors = BusinessSetupRules.validatePayments(
            SetupForm(upiId = "nope", accountNo = "12", ifsc = "X", prefix = "ABC"),
        )
        assertEquals(setOf(SetupField.UPI, SetupField.ACCOUNT_NO, SetupField.IFSC, SetupField.PREFIX), errors.keys)
    }

    @Test fun stepsAreDispatchedByIndex() {
        assertTrue(BusinessSetupRules.validateStep(0, SetupForm()).isNotEmpty())
        assertTrue(BusinessSetupRules.validateStep(1, SetupForm()).isNotEmpty())
        assertTrue(BusinessSetupRules.validateStep(2, SetupForm()).isEmpty())
    }
}
