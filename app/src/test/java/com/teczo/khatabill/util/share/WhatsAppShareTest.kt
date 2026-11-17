package com.teczo.khatabill.util.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WhatsAppShareTest {
    @Test fun tenDigitMobileGetsTheCountryCode() = assertEquals("919845012345", WhatsAppShare.normalize("9845012345"))
    @Test fun spacesAndDashesAreRemoved() = assertEquals("919845012345", WhatsAppShare.normalize("98450-12345"))
    @Test fun plusAndCountryCodeAreKept() = assertEquals("919845012345", WhatsAppShare.normalize("+91 98450 12345"))
    @Test fun leadingZeroIsReplacedByTheCountryCode() = assertEquals("919845012345", WhatsAppShare.normalize("09845012345"))
    @Test fun noNumberGivesNull() {
        assertNull(WhatsAppShare.normalize(null))
        assertNull(WhatsAppShare.normalize("   "))
        assertNull(WhatsAppShare.normalize("abc"))
    }
}
