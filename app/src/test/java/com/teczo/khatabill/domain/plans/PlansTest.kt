package com.teczo.khatabill.domain.plans

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlansTest {
    @Test fun mobilePricesAreOneYear() {
        assertEquals("1,699", PlanCatalog.priceText(PlanTier.RETAIL_PRO, PlanPlatform.MOBILE))
        assertEquals("999", PlanCatalog.priceText(PlanTier.GOLD, PlanPlatform.MOBILE))
    }

    @Test fun mobilePlusDesktopPrices() {
        assertEquals("10,479", PlanCatalog.priceText(PlanTier.RETAIL_PRO, PlanPlatform.MOBILE_DESKTOP))
        assertEquals("5,700", PlanCatalog.priceText(PlanTier.GOLD, PlanPlatform.MOBILE_DESKTOP))
    }

    @Test fun pricesAreStoredInPaise() {
        assertEquals(169_900L, PlanCatalog.yearlyPricePaise(PlanTier.RETAIL_PRO, PlanPlatform.MOBILE))
        assertEquals(1_047_900L, PlanCatalog.yearlyPricePaise(PlanTier.RETAIL_PRO, PlanPlatform.MOBILE_DESKTOP))
    }

    @Test fun proCostsMoreThanGoldAndTheBundleCostsMore() {
        for (platform in PlanPlatform.entries) {
            assertTrue(PlanCatalog.yearlyPricePaise(PlanTier.RETAIL_PRO, platform) > PlanCatalog.yearlyPricePaise(PlanTier.GOLD, platform))
        }
        for (tier in PlanTier.entries) {
            assertTrue(PlanCatalog.yearlyPricePaise(tier, PlanPlatform.MOBILE_DESKTOP) > PlanCatalog.yearlyPricePaise(tier, PlanPlatform.MOBILE))
        }
    }

    @Test fun goldNeverHasAFeatureThatProLacks() {
        PlanFeature.entries.forEach { assertTrue("${it.name} must be in Retail Pro if it is in Gold", !it.gold || it.pro) }
    }

    @Test fun featuresTheAppHasTodayAreInBothPlans() {
        PlanFeature.entries.filter { it.available }.forEach {
            assertTrue(it.name, it.includedIn(PlanTier.GOLD) && it.includedIn(PlanTier.RETAIL_PRO))
        }
        assertFalse(PlanFeature.LOYALTY.includedIn(PlanTier.GOLD))
    }

    @Test fun aPlanRunsForOneYearFromTheDayItStarts() {
        val start = java.time.LocalDate.of(2026, 11, 18).toEpochDay()
        val plan = ActivePlan.startingOn(PlanTier.GOLD, PlanPlatform.MOBILE, start)
        assertEquals(java.time.LocalDate.of(2027, 11, 18).toEpochDay(), plan.expiresDay)
        assertEquals(365L, plan.daysLeft(start))
        assertTrue(plan.isActive(start))
        assertTrue(plan.isActive(plan.expiresDay - 1))
        assertFalse(plan.isActive(plan.expiresDay))
        assertEquals(0L, plan.daysLeft(plan.expiresDay + 30))
    }

    @Test fun leapYearPlanHasThe366Days() {
        val start = java.time.LocalDate.of(2027, 3, 1).toEpochDay()
        assertEquals(366L, ActivePlan.startingOn(PlanTier.RETAIL_PRO, PlanPlatform.MOBILE, start).daysLeft(start))
    }
}
