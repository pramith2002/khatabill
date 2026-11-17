package com.teczo.khatabill.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
 * A floating pill bar. The selected tab sits in a soft circle, and Billing is the
 * raised round orange button in the centre.
 */
@Composable
fun AppBottomBar(selectedRoute: String?, onSelect: (String) -> Unit) {
    val pill = RoundedCornerShape(36.dp)
    Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).navigationBarsPadding().padding(bottom = 8.dp)) {
        Box(
            Modifier.fillMaxWidth().padding(top = 18.dp)
                .shadow(16.dp, pill)
                .background(MaterialTheme.colorScheme.surface, pill)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), pill),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
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
        Box(
            Modifier.size(width = 52.dp, height = 32.dp).background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                CircleShape,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        }
        Text(stringResource(tab.label), style = MaterialTheme.typography.labelMedium, color = tint, modifier = Modifier.padding(top = 2.dp))
    }
}
