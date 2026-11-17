package com.teczo.khatabill.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Where the app should open: decided from the saved language and whether a business exists. */
enum class StartState { LOADING, NEEDS_LANGUAGE, NEEDS_BUSINESS, READY }

@HiltViewModel
class StartViewModel @Inject constructor(
    settings: SettingsRepository,
    business: BusinessRepository,
) : ViewModel() {
    val state: StateFlow<StartState> = combine(settings.language, business.observeActive()) { lang, biz ->
        when {
            lang.isEmpty() -> StartState.NEEDS_LANGUAGE
            biz == null -> StartState.NEEDS_BUSINESS
            else -> StartState.READY
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StartState.LOADING)
}
