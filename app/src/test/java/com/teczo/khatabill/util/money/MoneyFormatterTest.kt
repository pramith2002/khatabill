package com.teczo.khatabill.util.money

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {

    @Test fun zero() = assertEquals("0.00", MoneyFormatter.format(0))
    @Test fun onePaisa() = assertEquals("0.01", MoneyFormatter.format(1))
    @Test fun tenPaise() = assertEquals("0.10", MoneyFormatter.format(10))
    @Test fun oneRupee() = assertEquals("1.00", MoneyFormatter.format(100))
    @Test fun upTo999() = assertEquals("999.99", MoneyFormatter.format(99_999))
    @Test fun thousand() = assertEquals("1,000.00", MoneyFormatter.format(100_000))
    @Test fun tenThousand() = assertEquals("10,000.00", MoneyFormatter.format(1_000_000))
    @Test fun lakh() = assertEquals("1,00,000.00", MoneyFormatter.format(10_000_000))
    @Test fun specExample() = assertEquals("1,23,456.00", MoneyFormatter.format(12_345_600))
    @Test fun crore() = assertEquals("1,00,00,000.00", MoneyFormatter.format(1_000_000_000))
    @Test fun withPaise() = assertEquals("12,400.50", MoneyFormatter.format(1_240_050))
    @Test fun negativeSmall() = assertEquals("-0.05", MoneyFormatter.format(-5))
    @Test fun negativeLarge() = assertEquals("-1,23,456.00", MoneyFormatter.format(-12_345_600))
    @Test fun maxLongIsExact() =
        assertEquals("92,23,37,20,36,85,47,758.07", MoneyFormatter.format(Long.MAX_VALUE))
    @Test fun minLongDoesNotCrash() =
        assertEquals("-92,23,37,20,36,85,47,758.08", MoneyFormatter.format(Long.MIN_VALUE))
}
