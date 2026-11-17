package com.teczo.khatabill.util.validators

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldValidatorsTest {
    @Test fun mobile() {
        assertTrue(FieldValidators.isValidMobile("9845012345"))
        assertTrue(FieldValidators.isValidMobile("6000000000"))
        assertFalse(FieldValidators.isValidMobile("5845012345"))
        assertFalse(FieldValidators.isValidMobile("984501234"))
        assertFalse(FieldValidators.isValidMobile("98450123456"))
        assertFalse(FieldValidators.isValidMobile("98450 12345"))
    }

    @Test fun pincode() {
        assertTrue(FieldValidators.isValidPincode("570001"))
        assertFalse(FieldValidators.isValidPincode("070001"))
        assertFalse(FieldValidators.isValidPincode("57001"))
    }

    @Test fun ifsc() {
        assertTrue(FieldValidators.isValidIfsc("SBIN0001234"))
        assertTrue(FieldValidators.isValidIfsc("sbin0001234"))
        assertFalse(FieldValidators.isValidIfsc("SBIN1001234"))
        assertFalse(FieldValidators.isValidIfsc("SBIN000123"))
    }

    @Test fun upi() {
        assertTrue(FieldValidators.isValidUpi("ramesh@okhdfcbank"))
        assertTrue(FieldValidators.isValidUpi("9845012345@ybl"))
        assertTrue(FieldValidators.isValidUpi("shop.name-1@upi"))
        assertFalse(FieldValidators.isValidUpi("ramesh"))
        assertFalse(FieldValidators.isValidUpi("@ybl"))
        assertFalse(FieldValidators.isValidUpi("a@b"))
    }

    @Test fun pan() {
        assertTrue(FieldValidators.isValidPan("AAPFU0939F"))
        assertFalse(FieldValidators.isValidPan("AAPFU09390"))
        assertFalse(FieldValidators.isValidPan("AAPF0939F"))
    }

    @Test fun accountNumber() {
        assertTrue(FieldValidators.isValidAccountNo("123456789012"))
        assertFalse(FieldValidators.isValidAccountNo("12345"))
        assertFalse(FieldValidators.isValidAccountNo("12345A7890"))
    }

    @Test fun prefixAtMostTwoCharacters() {
        assertTrue(FieldValidators.isValidPrefix(""))
        assertTrue(FieldValidators.isValidPrefix("KB"))
        assertTrue(FieldValidators.isValidPrefix("A1"))
        assertFalse(FieldValidators.isValidPrefix("KBX"))
        assertFalse(FieldValidators.isValidPrefix("K-"))
    }
}
