package com.teczo.khatabill.ui.onboarding

import com.teczo.khatabill.domain.model.BusinessType
import com.teczo.khatabill.domain.model.GstScheme
import com.teczo.khatabill.util.validators.FieldValidators
import com.teczo.khatabill.util.validators.GstinValidator

enum class SetupField {
    SHOP_NAME, OWNER_NAME, MOBILE, ADDRESS, CITY, PINCODE,
    GSTIN, PAN, STATE,
    UPI, ACCOUNT_NO, IFSC, PREFIX,
}

enum class FieldError {
    REQUIRED, MOBILE, PINCODE, GSTIN_LENGTH, GSTIN_FORMAT, GSTIN_STATE, GSTIN_CHECKSUM,
    PAN, UPI, ACCOUNT_NO, IFSC, PREFIX,
}

data class SetupForm(
    val shopName: String = "",
    val ownerName: String = "",
    val mobile: String = "",
    val address: String = "",
    val city: String = "",
    val pincode: String = "",
    val gstScheme: GstScheme = GstScheme.UNREGISTERED,
    val gstin: String = "",
    val pan: String = "",
    val stateCode: String = "",
    val businessType: BusinessType = BusinessType.GENERAL_TRADER,
    val upiId: String = "",
    val bankName: String = "",
    val accountNo: String = "",
    val ifsc: String = "",
    val prefix: String = "",
) {
    val chargesGst: Boolean get() = gstScheme != GstScheme.UNREGISTERED
}

/** Pure validation for the 3 setup steps, so it can be unit tested without any UI. */
object BusinessSetupRules {

    fun validateShop(f: SetupForm): Map<SetupField, FieldError> = buildMap {
        if (f.shopName.isBlank()) put(SetupField.SHOP_NAME, FieldError.REQUIRED)
        if (f.ownerName.isBlank()) put(SetupField.OWNER_NAME, FieldError.REQUIRED)
        if (f.mobile.isBlank()) put(SetupField.MOBILE, FieldError.REQUIRED)
        else if (!FieldValidators.isValidMobile(f.mobile.trim())) put(SetupField.MOBILE, FieldError.MOBILE)
        if (f.address.isBlank()) put(SetupField.ADDRESS, FieldError.REQUIRED)
        if (f.city.isBlank()) put(SetupField.CITY, FieldError.REQUIRED)
        if (f.pincode.isBlank()) put(SetupField.PINCODE, FieldError.REQUIRED)
        else if (!FieldValidators.isValidPincode(f.pincode.trim())) put(SetupField.PINCODE, FieldError.PINCODE)
    }

    fun validateGst(f: SetupForm): Map<SetupField, FieldError> = buildMap {
        if (f.chargesGst) {
            if (f.gstin.isBlank()) {
                put(SetupField.GSTIN, FieldError.REQUIRED)
            } else {
                val r = GstinValidator.validate(f.gstin)
                if (r is GstinValidator.Result.Invalid) put(SetupField.GSTIN, r.reason.toFieldError())
            }
        } else if (f.pan.isNotBlank() && !FieldValidators.isValidPan(f.pan.trim())) {
            put(SetupField.PAN, FieldError.PAN)
        }
        if (f.stateCode.isBlank()) put(SetupField.STATE, FieldError.REQUIRED)
    }

    fun validatePayments(f: SetupForm): Map<SetupField, FieldError> = buildMap {
        if (f.upiId.isNotBlank() && !FieldValidators.isValidUpi(f.upiId.trim())) put(SetupField.UPI, FieldError.UPI)
        if (f.accountNo.isNotBlank() && !FieldValidators.isValidAccountNo(f.accountNo.trim())) {
            put(SetupField.ACCOUNT_NO, FieldError.ACCOUNT_NO)
        }
        if (f.ifsc.isNotBlank() && !FieldValidators.isValidIfsc(f.ifsc.trim())) put(SetupField.IFSC, FieldError.IFSC)
        if (!FieldValidators.isValidPrefix(f.prefix.trim())) put(SetupField.PREFIX, FieldError.PREFIX)
    }

    fun validateStep(step: Int, f: SetupForm): Map<SetupField, FieldError> = when (step) {
        0 -> validateShop(f)
        1 -> validateGst(f)
        else -> validatePayments(f)
    }

    /** When the GSTIN is valid, the state and PAN come from it so they can never disagree. */
    fun applyGstin(f: SetupForm): SetupForm {
        val r = GstinValidator.validate(f.gstin)
        return if (r is GstinValidator.Result.Valid) f.copy(stateCode = r.stateCode, pan = r.pan) else f
    }

    private fun GstinValidator.Reason.toFieldError() = when (this) {
        GstinValidator.Reason.BAD_LENGTH -> FieldError.GSTIN_LENGTH
        GstinValidator.Reason.BAD_FORMAT -> FieldError.GSTIN_FORMAT
        GstinValidator.Reason.BAD_STATE -> FieldError.GSTIN_STATE
        GstinValidator.Reason.BAD_CHECKSUM -> FieldError.GSTIN_CHECKSUM
    }
}
