package com.teczo.khatabill.util.validators

/** Format checks for form fields. All functions expect already-trimmed input. */
object FieldValidators {
    private val MOBILE = Regex("^[6-9][0-9]{9}$")
    private val PINCODE = Regex("^[1-9][0-9]{5}$")
    private val IFSC = Regex("^[A-Z]{4}0[A-Z0-9]{6}$")
    private val UPI = Regex("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$")
    private val PAN = Regex("^[A-Z]{5}[0-9]{4}[A-Z]$")
    private val ACCOUNT = Regex("^[0-9]{6,20}$")
    private val PREFIX = Regex("^[A-Za-z0-9]{0,2}$")

    fun isValidMobile(s: String) = MOBILE.matches(s)
    fun isValidPincode(s: String) = PINCODE.matches(s)
    fun isValidIfsc(s: String) = IFSC.matches(s.uppercase())
    fun isValidUpi(s: String) = UPI.matches(s)
    fun isValidPan(s: String) = PAN.matches(s.uppercase())
    fun isValidAccountNo(s: String) = ACCOUNT.matches(s)

    /** Invoice prefix: at most 2 letters or digits, so numbers stay within the 16-character GST limit. */
    fun isValidPrefix(s: String) = PREFIX.matches(s)
}
