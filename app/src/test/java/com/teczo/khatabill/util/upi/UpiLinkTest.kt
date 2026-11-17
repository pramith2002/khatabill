package com.teczo.khatabill.util.upi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UpiLinkTest {
    @Test fun buildsTheStandardLink() = assertEquals(
        "upi://pay?pa=ramesh@okhdfcbank&pn=Sri%20Ganesh%20Agarbatti&am=1157.00&cu=INR&tn=INV%2F26-27%2F0001",
        UpiLink.build("ramesh@okhdfcbank", "Sri Ganesh Agarbatti", 115_700, "INV/26-27/0001"),
    )

    @Test fun amountAlwaysHasTwoDecimals() {
        assertEquals("1.05", UpiLink.amountText(105))
        assertEquals("0.05", UpiLink.amountText(5))
        assertEquals("1200.50", UpiLink.amountText(120_050))
        assertEquals("1.00", UpiLink.amountText(100))
    }

    @Test fun amountHasNoRoundingError() = assertEquals("12345678.99", UpiLink.amountText(1_234_567_899))

    @Test fun specialCharactersAreEncoded() {
        assertEquals("A%26B%20%3D%20C", UpiLink.encode("A&B = C"))
        assertEquals("100%25", UpiLink.encode("100%"))
        assertEquals("a-b._c~", UpiLink.encode("a-b._c~"))
    }

    @Test fun kannadaNamesAreEncodedAsUtf8() {
        val link = UpiLink.build("a@ybl", "ಶ್ರೀ", 100, "x")
        assertTrue(link, link.contains("pn=%E0%B2%B6%E0%B3%8D%E0%B2%B0%E0%B3%80"))
        assertTrue(link.all { it.code < 128 })
    }

    @Test fun upiIdKeepsItsAtSign() = assertTrue(UpiLink.build("name@bank", "N", 100, "x").contains("pa=name@bank&"))

    @Test fun spacesAroundValuesAreTrimmed() =
        assertTrue(UpiLink.build("  a@b  ", "  Shop  ", 100, "  note ").contains("pa=a@b&pn=Shop&am=1.00&cu=INR&tn=note"))

    @Test fun missingIdOrAmountIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { UpiLink.build("", "N", 100, "x") }
        assertThrows(IllegalArgumentException::class.java) { UpiLink.build("a@b", "N", 0, "x") }
    }
}
