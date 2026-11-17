package com.teczo.khatabill.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.teczo.khatabill.domain.plans.ActivePlan
import com.teczo.khatabill.domain.plans.PlanPlatform
import com.teczo.khatabill.domain.plans.PlanTier
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    /** App language as a BCP-47 tag. Empty string means "not chosen yet" (show the language picker). */
    val language: Flow<String> = dataStore.data.map { it[LANGUAGE] ?: "" }

    suspend fun setLanguage(tag: String) {
        dataStore.edit { it[LANGUAGE] = tag }
    }

    /** Daily due-date notifications. Off until the user switches it on (and allows notifications). */
    val remindersEnabled: Flow<Boolean> = dataStore.data.map { it[REMINDERS] ?: false }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[REMINDERS] = enabled }
    }

    /** The plan switched on for this phone, or null when none has been chosen. */
    val activePlan: Flow<ActivePlan?> = dataStore.data.map { prefs ->
        val tier = prefs[PLAN_TIER]?.let { runCatching { PlanTier.valueOf(it) }.getOrNull() }
        val platform = prefs[PLAN_PLATFORM]?.let { runCatching { PlanPlatform.valueOf(it) }.getOrNull() }
        val start = prefs[PLAN_START]
        val expires = prefs[PLAN_EXPIRES]
        if (tier == null || platform == null || start == null || expires == null) null else ActivePlan(tier, platform, start, expires)
    }

    suspend fun setActivePlan(plan: ActivePlan) {
        dataStore.edit {
            it[PLAN_TIER] = plan.tier.name
            it[PLAN_PLATFORM] = plan.platform.name
            it[PLAN_START] = plan.startDay
            it[PLAN_EXPIRES] = plan.expiresDay
        }
    }

    private companion object {
        val PLAN_TIER = stringPreferencesKey("plan_tier")
        val PLAN_PLATFORM = stringPreferencesKey("plan_platform")
        val PLAN_START = longPreferencesKey("plan_start_day")
        val PLAN_EXPIRES = longPreferencesKey("plan_expires_day")
        val LANGUAGE = stringPreferencesKey("language")
        val REMINDERS = booleanPreferencesKey("reminders_enabled")
    }
}
