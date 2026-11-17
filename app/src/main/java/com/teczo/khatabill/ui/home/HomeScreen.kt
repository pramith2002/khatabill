package com.teczo.khatabill.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.teczo.khatabill.ui.components.StatTile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teczo.khatabill.R
import com.teczo.khatabill.ui.components.AccentButton
import com.teczo.khatabill.ui.components.PeachCard
import com.teczo.khatabill.ui.components.ProgressRing
import com.teczo.khatabill.ui.components.TealCard
import com.teczo.khatabill.ui.components.WhiteCard
import com.teczo.khatabill.util.dates.DateFormat
import com.teczo.khatabill.util.money.MoneyFormatter

@Composable
private fun rupees(paise: Long) = stringResource(R.string.amount_rupees, MoneyFormatter.format(paise))

@Composable
fun HomeScreen(
    onCollections: () -> Unit,
    onNewSale: () -> Unit,
    onQuickBill: () -> Unit,
    onAddPayment: () -> Unit,
    onAddCustomer: () -> Unit,
    onOpenInvoice: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state.summary
    val pageBackground = com.teczo.khatabill.ui.theme.pageBackground()

    Column(
        Modifier.fillMaxSize().background(pageBackground).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.home_greeting, state.ownerName),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        TealCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.home_to_collect), style = MaterialTheme.typography.titleMedium)
                    Text(rupees(s.toCollect), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                    AccentButton(onClick = onCollections) { Text(stringResource(R.string.home_view_collections), style = MaterialTheme.typography.labelLarge) }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ProgressRing(s.collectionRatio)
                    Text(stringResource(R.string.home_collected), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            PeachCard(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_to_pay), style = MaterialTheme.typography.titleSmall)
                Text(rupees(s.toPay), style = MaterialTheme.typography.titleLarge)
            }
            WhiteCard(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_overdue), style = MaterialTheme.typography.titleSmall)
                Text(rupees(s.overdue), style = MaterialTheme.typography.titleLarge, color = if (s.overdue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
        }

        Text(stringResource(R.string.dash_today), style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile(stringResource(R.string.dash_sales), rupees(s.todaySales), Modifier.weight(1f), trend = s.salesLast7)
            StatTile(stringResource(R.string.dash_collected), rupees(s.todayCollected), Modifier.weight(1f), trend = s.collectedLast7, peach = true)
        }
        StatTile(stringResource(R.string.dash_month_sales), rupees(s.monthSales), Modifier.fillMaxWidth())

        Text(stringResource(R.string.home_quick_actions), style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            QuickAction(Icons.Filled.Add, R.string.qa_new_sale, onNewSale, Modifier.weight(1f))
            QuickAction(Icons.Filled.Check, R.string.qa_add_payment, onAddPayment, Modifier.weight(1f))
            QuickAction(Icons.Filled.Person, R.string.qa_add_customer, onAddCustomer, Modifier.weight(1f))
        }
        QuickAction(Icons.Filled.ShoppingCart, R.string.qa_quick_bill, onQuickBill, Modifier.fillMaxWidth())

        DashList(R.string.dash_top_items, state.topItems)
        DashList(R.string.dash_top_customers, state.topCustomers)
        if (state.lowStock.isNotEmpty()) DashList(R.string.item_low_stock, state.lowStock, valueColor = MaterialTheme.colorScheme.error)

        Text(stringResource(R.string.dash_recent), style = MaterialTheme.typography.titleLarge)
        if (state.recent.isEmpty()) Text(stringResource(R.string.dash_none), style = MaterialTheme.typography.bodyMedium)
        state.recent.forEach { inv ->
            WhiteCard(Modifier.fillMaxWidth().clickable { onOpenInvoice(inv.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(inv.number, style = MaterialTheme.typography.titleMedium)
                        Text(inv.partyName ?: stringResource(R.string.walk_in_customer), style = MaterialTheme.typography.bodySmall)
                        Text(DateFormat.format(inv.date), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(rupees(inv.grandTotal), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun DashList(titleRes: Int, rows: List<DashRow>, valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Text(stringResource(titleRes), style = MaterialTheme.typography.titleLarge)
    WhiteCard(Modifier.fillMaxWidth()) {
        if (rows.isEmpty()) Text(stringResource(R.string.dash_none), style = MaterialTheme.typography.bodyMedium)
        rows.forEach { r ->
            Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(r.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(r.value, style = MaterialTheme.typography.titleMedium, color = valueColor)
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: Int, onClick: () -> Unit, modifier: Modifier) {
    PeachCard(modifier.heightIn(min = 96.dp).clickable(onClick = onClick)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
    }
}
