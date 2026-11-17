package com.teczo.khatabill.util.validators

import com.teczo.khatabill.util.validators.GstinValidator.Reason
import com.teczo.khatabill.util.validators.GstinValidator.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GstinValidatorTest {

    @Test fun validGstinReturnsStateAndPan() {
        val r = GstinValidator.validate("27AAPFU0939F1ZV") as Result.Valid
        assertEquals("27", r.stateCode)
        assertEquals("AAPFU0939F", r.pan)
    }

    @Test fun validKarnatakaGstin() {
        val r = GstinValidator.validate("29AAGCB7383J1Z4") as Result.Valid
        assertEquals("29", r.stateCode)
        assertEquals("Karnataka", GstStateCodes.englishName(r.stateCode))
    }

    @Test fun validDelhiGstin() {
        assertTrue(GstinValidator.validate("07AAGFF2194N1Z1") is Result.Valid)
    }

    @Test fun lowercaseAndSpacesAreAccepted() {
        val r = GstinValidator.validate("  27aapfu0939f1zv ") as Result.Valid
        assertEquals("27AAPFU0939F1ZV", r.gstin)
    }

    @Test fun wrongCheckCharacter() {
        assertEquals(Result.Invalid(Reason.BAD_CHECKSUM), GstinValidator.validate("27AAPFU0939F1ZW"))
    }

    @Test fun changedDigitBreaksChecksum() {
        assertEquals(Result.Invalid(Reason.BAD_CHECKSUM), GstinValidator.validate("27AAPFU0938F1ZV"))
    }

    @Test fun tooShort() {
        assertEquals(Result.Invalid(Reason.BAD_LENGTH), GstinValidator.validate("27AAPFU0939F1Z"))
    }

    @Test fun tooLong() {
        assertEquals(Result.Invalid(Reason.BAD_LENGTH), GstinValidator.validate("27AAPFU0939F1ZVX"))
    }

    @Test fun emptyInput() {
        assertEquals(Result.Invalid(Reason.BAD_LENGTH), GstinValidator.validate(""))
    }

    @Test fun fourteenthCharacterMustBeZ() {
        assertEquals(Result.Invalid(Reason.BAD_FORMAT), GstinValidator.validate("27AAPFU0939F1XV"))
    }

    @Test fun panPartMustHaveLettersAndDigitsInPlace() {
        assertEquals(Result.Invalid(Reason.BAD_FORMAT), GstinValidator.validate("27111110939F1ZV"))
    }

    @Test fun unknownStateCode() {
        assertEquals(Result.Invalid(Reason.BAD_STATE), GstinValidator.validate("39AAPFU0939F1ZV"))
        assertEquals(Result.Invalid(Reason.BAD_STATE), GstinValidator.validate("00AAPFU0939F1ZV"))
    }

    @Test fun checkCharacterMatchesKnownGstins() {
        assertEquals('V', GstinValidator.checkCharacter("27AAPFU0939F1Z"))
        assertEquals('4', GstinValidator.checkCharacter("29AAGCB7383J1Z"))
    }

    @Test fun anyGeneratedGstinWithItsCheckCharacterIsValid() {
        val first14 = "29ABCDE1234F1Z"
        val gstin = first14 + GstinValidator.checkCharacter(first14)
        assertTrue(GstinValidator.validate(gstin) is Result.Valid)
    }

    @Test fun stateCodeTableHasKarnatakaAndRejectsUnknown() {
        assertTrue(GstStateCodes.isValid("29"))
        assertTrue(!GstStateCodes.isValid("25"))
    }
}
