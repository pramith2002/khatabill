package com.teczo.khatabill.domain.invoice

import com.teczo.khatabill.domain.model.DocumentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InvoiceNumberTest {
    @Test fun basicFormat() = assertEquals("INV/26-27/0001", InvoiceNumber.format("", "INV", "2026-27", 1))

    @Test fun withPrefix() = assertEquals("KBINV/26-27/0042", InvoiceNumber.format("KB", "INV", "2026-27", 42))

    @Test fun largeSequenceGrowsBeyondFourDigits() =
        assertEquals("INV/26-27/12345", InvoiceNumber.format("", "INV", "2026-27", 12_345))

    @Test fun twoCharacterPrefixStaysWithinGstLimitOfSixteen() {
        val n = InvoiceNumber.format("KB", "INV", "2026-27", 9_999)
        assertTrue(n.length <= 16)
    }

    @Test fun threeCharacterPrefixExceedsTheLimitSoSetupMustRejectIt() {
        val n = InvoiceNumber.format("KBX", "INV", "2026-27", 9_999)
        assertTrue(n.length > 16)
    }

    @Test fun seriesPerDocumentType() {
        assertEquals("INV", InvoiceNumber.defaultSeries(DocumentType.SALE, chargesTax = true))
        assertEquals("BOS", InvoiceNumber.defaultSeries(DocumentType.SALE, chargesTax = false))
        assertEquals("QUO", InvoiceNumber.defaultSeries(DocumentType.QUOTATION, chargesTax = true))
        assertEquals("PUR", InvoiceNumber.defaultSeries(DocumentType.PURCHASE, chargesTax = true))
    }
}
