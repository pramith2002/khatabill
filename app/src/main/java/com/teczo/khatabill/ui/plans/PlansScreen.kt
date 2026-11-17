package com.teczo.khatabill.ui.plans

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teczo.khatabill.R
import com.teczo.khatabill.domain.plans.PlanCatalog
import com.teczo.khatabill.domain.plans.PlanFeature
import com.teczo.khatabill.domain.plans.PlanPlatform
import com.teczo.khatabill.domain.plans.PlanTier
import com.teczo.khatabill.ui.components.AccentButton
import com.teczo.khatabill.ui.components.PeachCard
import com.teczo.khatabill.ui.components.ScreenHeader
import com.teczo.khatabill.util.dates.DateFormat

private val CheckGreen = Color(0xFF1E9E63)
private val CrossRed = Color(0xFFD64545)
private val ColumnWidth = 84.dp

@StringRes
private fun tierName(tier: PlanTier) = if (tier == PlanTier.GOLD) R.string.plan_gold else R.string.plan_retail_pro

@StringRes
private fun platformName(platform: PlanPlatform) =
    if (platform == PlanPlatform.MOBILE) R.string.plans_platform_mobile else R.string.plans_platform_both

@StringRes
private fun featureLabel(f: PlanFeature) = when (f) {
    PlanFeature.GST_BILLING -> R.string.feat_gst_billing
    PlanFeature.REPORTS -> R.string.feat_reports
    PlanFeature.UDHAAR -> R.string.feat_udhaar
    PlanFeature.PAYMENTS_UPI -> R.string.feat_payments_upi
    PlanFeature.BOOKS -> R.string.feat_books
    PlanFeature.QUICK_BILL_SCAN -> R.string.feat_quick_bill_scan
    PlanFeature.LANGUAGES -> R.string.feat_languages
    PlanFeature.OFFLINE -> R.string.feat_offline
    PlanFeature.COMPANIES -> R.string.feat_companies
    PlanFeature.BULK_WHATSAPP -> R.string.feat_bulk_whatsapp
    PlanFeature.GREETINGS -> R.string.feat_greetings
    PlanFeature.LOYALTY -> R.string.feat_loyalty
    PlanFeature.MARKETING -> R.string.feat_marketing
    PlanFeature.MULTI_CURRENCY -> R.string.feat_multi_currency
}

/**
 * Plans and pricing: a 1-year plan for Mobile or Mobile + Desktop, a Gold / Retail Pro feature table, and the
 * two prices. Buying inside the app is not built yet, so the button only says so.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlansScreen(onBack: () -> Unit, viewModel: PlansViewModel = hiltViewModel()) {
    val active by viewModel.activePlan.collectAsStateWithLifecycle()
    val today = DateFormat.today()
    val current = active?.takeIf { it.isActive(today) }
    var platform by rememberSaveable { mutableStateOf(PlanPlatform.MOBILE) }
    var selected by rememberSaveable { mutableStateOf(PlanTier.RETAIL_PRO) }
    var confirm by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        ScreenHeader(stringResource(R.string.plans_title), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (current != null) {
                PeachCard(Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.plans_current, stringResource(tierName(current.tier)), stringResource(platformName(current.platform))),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.plans_days_left, current.daysLeft(today).toInt(), DateFormat.format(current.expiresDay)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            OutlinedCard(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.plans_one_year), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(PlanPlatform.MOBILE to R.string.plans_platform_mobile, PlanPlatform.MOBILE_DESKTOP to R.string.plans_platform_both)
                            .forEach { (p, label) ->
                                FilterChip(
                                    selected = platform == p,
                                    onClick = { platform = p },
                                    label = { Text(stringResource(label), style = MaterialTheme.typography.labelLarge) },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                )
                            }
                    }
                    if (platform == PlanPlatform.MOBILE_DESKTOP) {
                        Text(stringResource(R.string.plans_desktop_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.plans_features_header),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                PlanTier.entries.forEach { tier ->
                    Text(
                        stringResource(tierName(tier)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(ColumnWidth),
                    )
                }
            }
            HorizontalDivider()
            PlanFeature.entries.forEach { feature ->
                FeatureRow(feature)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlanTier.entries.forEach { tier ->
                    PlanPriceCard(tier, platform, selected == tier, { selected = tier }, Modifier.weight(1f))
                }
            }
            val isCurrent = current != null && current.tier == selected && current.platform == platform
            AccentButton(onClick = { confirm = true }, enabled = !isCurrent, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (isCurrent) stringResource(R.string.plans_current_button) else stringResource(R.string.plans_get, stringResource(tierName(selected))),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.plans_test_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.plans_test_message,
                        stringResource(tierName(selected)),
                        stringResource(platformName(platform)),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.activate(selected, platform); confirm = false }) { Text(stringResource(R.string.plans_activate)) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.btn_cancel)) } },
        )
    }
}

@Composable
private fun FeatureRow(feature: PlanFeature) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(stringResource(featureLabel(feature)), style = MaterialTheme.typography.bodyLarge)
            if (!feature.available) {
                Text(stringResource(R.string.plans_coming_soon), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            }
        }
        PlanTier.entries.forEach { tier ->
            Box(Modifier.width(ColumnWidth), contentAlignment = Alignment.Center) {
                if (feature == PlanFeature.COMPANIES) {
                    Text(
                        stringResource(if (tier == PlanTier.GOLD) R.string.feat_companies_gold else R.string.feat_companies_pro),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                } else if (feature.includedIn(tier)) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = CheckGreen)
                } else {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = CrossRed)
                }
            }
        }
    }
}

@Composable
private fun PlanPriceCard(tier: PlanTier, platform: PlanPlatform, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    OutlinedCard(modifier.clickable(onClick = onClick), border = border) {
        Column(Modifier.padding(14.dp)) {
            Text(stringResource(tierName(tier)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.amount_rupees, PlanCatalog.priceText(tier, platform)),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(stringResource(R.string.plans_per_year), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
