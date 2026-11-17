package com.teczo.khatabill.util.validators

/**
 * Validates a GSTIN: 15 characters = state code (2) + PAN (10) + entity number (1) + 'Z' + check character.
 * Checks run in this order: length, format, state code, checksum.
 */
object GstinValidator {

    enum class Reason { BAD_LENGTH, BAD_FORMAT, BAD_STATE, BAD_CHECKSUM }

    sealed interface Result {
        /** [stateCode] can be used to pre-fill the business or party state. */
        data class Valid(val gstin: String, val stateCode: String, val pan: String) : Result
        data class Invalid(val reason: Reason) : Result
    }

    private const val CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val FORMAT = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$")

    fun validate(input: String): Result {
        val gstin = input.trim().uppercase()
        if (gstin.length != 15) return Result.Invalid(Reason.BAD_LENGTH)
        if (!FORMAT.matches(gstin)) return Result.Invalid(Reason.BAD_FORMAT)
        val stateCode = gstin.substring(0, 2)
        if (!GstStateCodes.isValid(stateCode)) return Result.Invalid(Reason.BAD_STATE)
        if (checkCharacter(gstin.substring(0, 14)) != gstin[14]) return Result.Invalid(Reason.BAD_CHECKSUM)
        return Result.Valid(gstin = gstin, stateCode = stateCode, pan = gstin.substring(2, 12))
    }

    /** The 15th character for the first 14 characters of a GSTIN (base-36 weighted checksum). */
    fun checkCharacter(first14: String): Char {
        require(first14.length == 14) { "Expected 14 characters" }
        var sum = 0
        first14.forEachIndexed { i, c ->
            val value = CHARSET.indexOf(c)
            require(value >= 0) { "Invalid character '$c'" }
            val product = value * (if (i % 2 == 0) 1 else 2)
            sum += product / 36 + product % 36
        }
        return CHARSET[(36 - sum % 36) % 36]
    }
}
