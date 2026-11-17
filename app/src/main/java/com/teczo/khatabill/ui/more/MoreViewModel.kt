package com.teczo.khatabill.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {
    val remindersEnabled: StateFlow<Boolean> =
        settings.remindersEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setRemindersEnabled(enabled) }
    }
}
