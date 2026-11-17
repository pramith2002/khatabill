package com.teczo.khatabill.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitCatalogTest {
    @Test fun everyBusinessTypeUsesOnlyKnownUnits() {
        BusinessType.entries.forEach { type ->
            val units = UnitCatalog.unitsFor(type)
            assertTrue("$type has no units", units.isNotEmpty())
            assertTrue("$type has unknown units", UnitCatalog.all.containsAll(units))
            assertEquals("$type has duplicates", units.size, units.toSet().size)
        }
    }

    @Test fun agarbattiStartsWithWeight() = assertEquals("kg", UnitCatalog.unitsFor(BusinessType.AGARBATTI).first())

    @Test fun silkStartsWithMetre() = assertEquals("metre", UnitCatalog.unitsFor(BusinessType.SILK_HANDICRAFTS).first())

    @Test fun generalTraderGetsEveryUnit() =
        assertEquals(UnitCatalog.all, UnitCatalog.unitsFor(BusinessType.GENERAL_TRADER))

    @Test fun gstRatesAreSortedAndIncludeCommonSlabs() {
        assertEquals(GstRates.options.sorted(), GstRates.options)
        assertTrue(GstRates.options.containsAll(listOf(0, 500, 1200, 1800, 2800)))
    }
}
