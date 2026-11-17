package com.teczo.khatabill.domain.plans

import com.teczo.khatabill.util.money.MoneyFormatter

enum class PlanTier { GOLD, RETAIL_PRO }

/** Where the plan can be used. The desktop app is not built yet, so the bundle covers it once it ships. */
enum class PlanPlatform { MOBILE, MOBILE_DESKTOP }

/**
 * One row of the feature comparison.
 * [available] is false for features that are planned but not in the app yet (shown as "Coming soon").
 * [COMPANIES] shows a limit (5 or unlimited) instead of a tick.
 */
enum class PlanFeature(val gold: Boolean, val pro: Boolean, val available: Boolean) {
    GST_BILLING(gold = true, pro = true, available = true),
    REPORTS(gold = true, pro = true, available = true),
    UDHAAR(gold = true, pro = true, available = true),
    PAYMENTS_UPI(gold = true, pro = true, available = true),
    BOOKS(gold = true, pro = true, available = true),
    QUICK_BILL_SCAN(gold = true, pro = true, available = true),
    LANGUAGES(gold = true, pro = true, available = true),
    OFFLINE(gold = true, pro = true, available = true),

    COMPANIES(gold = true, pro = true, available = false),
    BULK_WHATSAPP(gold = false, pro = true, available = false),
    GREETINGS(gold = false, pro = true, available = false),
    LOYALTY(gold = false, pro = true, available = false),
    MARKETING(gold = false, pro = true, available = false),
    MULTI_CURRENCY(gold = false, pro = true, available = false),
    ;

    fun includedIn(tier: PlanTier): Boolean = if (tier == PlanTier.GOLD) gold else pro
}

/** Prices for a 1-year plan, in paise (so they format with the same Indian grouping as every other amount). */
object PlanCatalog {
    fun yearlyPricePaise(tier: PlanTier, platform: PlanPlatform): Long = when (platform) {
        PlanPlatform.MOBILE -> if (tier == PlanTier.GOLD) 99_900L else 169_900L
        PlanPlatform.MOBILE_DESKTOP -> if (tier == PlanTier.GOLD) 570_000L else 1_047_900L
    }

    /** "1,699" style text: whole rupees, no decimals. */
    fun priceText(tier: PlanTier, platform: PlanPlatform): String =
        MoneyFormatter.format(yearlyPricePaise(tier, platform)).removeSuffix(".00")
}

/**
 * A plan switched on for this phone. Days are epoch days. The plan runs for one calendar year from the day it
 * starts and stops being active on [expiresDay].
 */
data class ActivePlan(val tier: PlanTier, val platform: PlanPlatform, val startDay: Long, val expiresDay: Long) {
    fun isActive(today: Long): Boolean = today < expiresDay

    fun daysLeft(today: Long): Long = (expiresDay - today).coerceAtLeast(0)

    companion object {
        fun startingOn(tier: PlanTier, platform: PlanPlatform, today: Long) = ActivePlan(
            tier, platform, today, java.time.LocalDate.ofEpochDay(today).plusYears(1).toEpochDay(),
        )
    }
}
