package com.teczo.khatabill.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

    private companion object {
        val LANGUAGE = stringPreferencesKey("language")
        val REMINDERS = booleanPreferencesKey("reminders_enabled")
    }
}
