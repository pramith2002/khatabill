package com.teczo.khatabill.ui.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.data.settings.SettingsRepository
import com.teczo.khatabill.util.locale.AppLocale
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {

    /** null = still loading, "" = not chosen yet, otherwise a language tag ("en", "kn", "hi"). */
    val language: StateFlow<String?> =
        settings.language.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun select(tag: String) {
        viewModelScope.launch { settings.setLanguage(tag) }
        AppLocale.apply(tag)
    }
}
