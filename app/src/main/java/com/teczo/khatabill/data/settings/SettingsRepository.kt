package com.teczo.khatabill.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** App language as a BCP-47 tag. Empty string means "not chosen yet" (show the language picker). */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val language: Flow<String> = dataStore.data.map { it[LANGUAGE] ?: "" }

    suspend fun setLanguage(tag: String) {
        dataStore.edit { it[LANGUAGE] = tag }
    }

    private companion object {
        val LANGUAGE = stringPreferencesKey("language")
    }
}
