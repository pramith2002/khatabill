package com.teczo.khatabill.util.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AmountInWordsTest {
    private fun w(paise: Long) = AmountInWords.english(paise)

    @Test fun zero() = assertEquals("Rupees Zero Only", w(0))
    @Test fun oneRupee() = assertEquals("Rupees One Only", w(100))
    @Test fun teen() = assertEquals("Rupees Nineteen Only", w(1_900))
    @Test fun roundTens() = assertEquals("Rupees Forty Only", w(4_000))
    @Test fun tensAndOnes() = assertEquals("Rupees Seventy Seven Only", w(7_700))
    @Test fun hundreds() = assertEquals("Rupees Five Hundred Only", w(50_000))
    @Test fun hundredsAndTens() = assertEquals("Rupees One Hundred Eleven Only", w(11_100))
    @Test fun thousands() = assertEquals("Rupees Twelve Thousand Four Hundred Only", w(1_240_000))
    @Test fun lakhAndUp() = assertEquals(
        "Rupees One Lakh Twenty Three Thousand Four Hundred Fifty Six Only", w(12_345_600),
    )
    @Test fun lakhWithNoThousands() = assertEquals("Rupees Two Lakh Five Only", w(20_000_500))
    @Test fun crore() = assertEquals("Rupees One Crore Only", w(1_000_000_000))
    @Test fun hundredCroreUsesNestedCrore() = assertEquals("Rupees One Hundred Crore Only", w(100_00_00_000_00L))
    @Test fun paiseOnly() = assertEquals("Rupees Zero and Five Paise Only", w(5))
    @Test fun rupeesAndPaise() = assertEquals("Rupees One Hundred Eighteen and Fifty Paise Only", w(11_850))
    @Test fun negativeRejected() { assertThrows(IllegalArgumentException::class.java) { w(-1) } }
}
