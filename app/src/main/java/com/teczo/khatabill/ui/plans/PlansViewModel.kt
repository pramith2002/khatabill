package com.teczo.khatabill.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.data.settings.SettingsRepository
import com.teczo.khatabill.domain.plans.ActivePlan
import com.teczo.khatabill.domain.plans.PlanPlatform
import com.teczo.khatabill.domain.plans.PlanTier
import com.teczo.khatabill.util.dates.DateFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Test mode: a plan is switched on for this phone without a payment, for one year. Real purchases (Google Play
 * Billing) replace [activate] later.
 */
@HiltViewModel
class PlansViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {
    val activePlan: StateFlow<ActivePlan?> =
        settings.activePlan.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun activate(tier: PlanTier, platform: PlanPlatform) {
        viewModelScope.launch { settings.setActivePlan(ActivePlan.startingOn(tier, platform, DateFormat.today())) }
    }
}
