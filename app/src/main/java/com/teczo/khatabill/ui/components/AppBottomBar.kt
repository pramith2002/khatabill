package com.teczo.khatabill.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.teczo.khatabill.ui.navigation.Routes
import com.teczo.khatabill.ui.navigation.TabItem
import com.teczo.khatabill.ui.navigation.bottomTabs

/**
 * Bottom bar from the design reference: icons with a small label, a dot under the selected tab,
 * and a raised round orange button in the centre for Billing.
 */
@Composable
fun AppBottomBar(selectedRoute: String?, onSelect: (String) -> Unit) {
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth().padding(top = 18.dp)
                .shadow(14.dp, shape)
                .background(MaterialTheme.colorScheme.surface, shape),
        ) {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                bottomTabs.forEach { tab ->
                    if (tab.route == Routes.BILLING) {
                        Spacer(Modifier.weight(1f)) // the raised button is drawn on top of this slot
                    } else {
                        NormalTab(tab, selected = selectedRoute == tab.route, onClick = { onSelect(tab.route) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        val billing = bottomTabs.first { it.route == Routes.BILLING }
        Column(
            Modifier.align(Alignment.TopCenter).heightIn(min = 48.dp)
                .clickable(role = Role.Tab, onClick = { onSelect(Routes.BILLING) }),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(62.dp).shadow(10.dp, CircleShape).background(MaterialTheme.colorScheme.secondary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(billing.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(32.dp))
            }
            Text(
                stringResource(billing.label),
                style = MaterialTheme.typography.labelMedium,
                color = if (selectedRoute == billing.route) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun NormalTab(tab: TabItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier.heightIn(min = 56.dp).clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
        Text(stringResource(tab.label), style = MaterialTheme.typography.labelMedium, color = tint)
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier.size(6.dp).background(
                if (selected) MaterialTheme.colorScheme.secondary else androidx.compose.ui.graphics.Color.Transparent,
                CircleShape,
            ),
        )
    }
}
