package com.teczo.khatabill.ui.more

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.teczo.khatabill.R
import com.teczo.khatabill.ui.components.WhiteCard
import com.teczo.khatabill.ui.language.LanguageOptions

data class MoreEntry(@StringRes val label: Int, val icon: ImageVector, val onClick: () -> Unit)

/** The More tab: shortcuts to money screens and reports, reminder switch, then the language choice. */
@Composable
fun MoreScreen(
    entries: List<MoreEntry>,
    currentLanguage: String,
    onLanguage: (String) -> Unit,
    remindersEnabled: Boolean,
    onRemindersChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var rationale by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> onRemindersChange(granted) }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.nav_more), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.more_section_money), style = MaterialTheme.typography.titleMedium)
        entries.forEach { e ->
            WhiteCard(Modifier.fillMaxWidth().clickable(onClick = e.onClick)) {
                Row(Modifier.heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(e.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(e.label), style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Text(stringResource(R.string.more_section_app), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        WhiteCard(Modifier.fillMaxWidth()) {
            Row(Modifier.heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.reminders_setting), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.reminders_setting_hint), style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = remindersEnabled,
                    onCheckedChange = { on ->
                        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        when {
                            !on -> onRemindersChange(false)
                            needsPermission -> rationale = true
                            else -> onRemindersChange(true)
                        }
                    },
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.Filled.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.language_section), style = MaterialTheme.typography.titleMedium)
        }
        LanguageOptions(current = currentLanguage, onSelect = onLanguage)
    }

    if (rationale) {
        AlertDialog(
            onDismissRequest = { rationale = false },
            title = { Text(stringResource(R.string.notif_rationale_title)) },
            text = { Text(stringResource(R.string.notif_rationale_message)) },
            confirmButton = {
                TextButton(onClick = { rationale = false; permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text(stringResource(R.string.btn_allow)) }
            },
            dismissButton = { TextButton(onClick = { rationale = false }) { Text(stringResource(R.string.btn_not_now)) } },
        )
    }
}

/** Icon for money rows until each screen has its own. */
val moneyIcon: ImageVector get() = Icons.Filled.Check
