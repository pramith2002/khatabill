package com.teczo.khatabill.util.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyInputTest {
    @Test fun wholeRupees() = assertEquals(12_300L, MoneyInput.parsePaise("123"))
    @Test fun rupeesAndPaise() = assertEquals(12_345L, MoneyInput.parsePaise("123.45"))
    @Test fun singleDecimalDigitMeansTenths() = assertEquals(12_340L, MoneyInput.parsePaise("123.4"))
    @Test fun trailingDotIsAccepted() = assertEquals(12_300L, MoneyInput.parsePaise("123."))
    @Test fun leadingDotIsAccepted() = assertEquals(50L, MoneyInput.parsePaise(".5"))
    @Test fun spacesAreTrimmed() = assertEquals(10_000L, MoneyInput.parsePaise("  100 "))
    @Test fun zero() = assertEquals(0L, MoneyInput.parsePaise("0"))
    @Test fun blankIsNull() = assertNull(MoneyInput.parsePaise("  "))
    @Test fun threeDecimalsRejectedForMoney() = assertNull(MoneyInput.parsePaise("1.234"))
    @Test fun negativeRejected() = assertNull(MoneyInput.parsePaise("-5"))
    @Test fun lettersRejected() = assertNull(MoneyInput.parsePaise("12a"))
    @Test fun twoDotsRejected() = assertNull(MoneyInput.parsePaise("1.2.3"))
    @Test fun dotAloneRejected() = assertNull(MoneyInput.parsePaise("."))
    @Test fun absurdlyLargeRejected() = assertNull(MoneyInput.parsePaise("1234567890123"))
    @Test fun commaRejected() = assertNull(MoneyInput.parsePaise("1,200"))

    @Test fun quantityThousandths() {
        assertEquals(1_250L, MoneyInput.parseQty("1.25"))
        assertEquals(1_250L, MoneyInput.parseQty("1.250"))
        assertEquals(5_000L, MoneyInput.parseQty("5"))
        assertEquals(1L, MoneyInput.parseQty("0.001"))
        assertNull(MoneyInput.parseQty("1.2345"))
    }

    @Test fun percentBasisPoints() {
        assertEquals(1_800, MoneyInput.parsePercentBp("18"))
        assertEquals(250, MoneyInput.parsePercentBp("2.5"))
        assertEquals(25, MoneyInput.parsePercentBp("0.25"))
        assertEquals(10_000, MoneyInput.parsePercentBp("100"))
        assertNull(MoneyInput.parsePercentBp("100.01"))
        assertNull(MoneyInput.parsePercentBp("101"))
    }

    @Test fun textRoundTrips() {
        assertEquals("123.45", MoneyInput.paiseToText(12_345))
        assertEquals("123", MoneyInput.paiseToText(12_300))
        assertEquals("123.5", MoneyInput.paiseToText(12_350))
        assertEquals("0.05", MoneyInput.paiseToText(5))
        assertEquals("0", MoneyInput.paiseToText(0))
        assertEquals("1.25", MoneyInput.qtyToText(1_250))
        assertEquals("0.001", MoneyInput.qtyToText(1))
        assertEquals("18", MoneyInput.bpToText(1_800))
        assertEquals("2.5", MoneyInput.bpToText(250))
        assertEquals("0.25", MoneyInput.bpToText(25))
    }

    @Test fun parseThenFormatIsStable() {
        listOf("0.01", "7", "99.9", "123456.78").forEach { s ->
            assertEquals(s, MoneyInput.paiseToText(MoneyInput.parsePaise(s)!!))
        }
    }
}
