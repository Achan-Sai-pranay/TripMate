package com.example.tripmate.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.tripmate.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDarkMode by remember { mutableStateOf(true) }
    var pushNotifications by remember { mutableStateOf(true) }
    var emailNotifications by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Account Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Dimens.lg),
            verticalArrangement = Arrangement.spacedBy(Dimens.xl)
        ) {
            SettingSection(title = "Appearance") {
                SettingToggleRow(
                    label = "Dark Mode",
                    description = "Enable dark theme",
                    checked = isDarkMode,
                    onCheckedChange = { isDarkMode = it }
                )
            }
            
            SettingSection(title = "Notifications") {
                SettingToggleRow(
                    label = "Push Notifications",
                    description = "Get notified about your trip updates",
                    checked = pushNotifications,
                    onCheckedChange = { pushNotifications = it }
                )
                SettingToggleRow(
                    label = "Email Updates",
                    description = "Receive weekly travel tips",
                    checked = emailNotifications,
                    onCheckedChange = { emailNotifications = it }
                )
            }
            
            SettingSection(title = "Preferences") {
                var showCurrencyInfoDialog by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCurrencyInfoDialog = true }
                        .padding(vertical = Dimens.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Default Currency", style = MaterialTheme.typography.bodyLarge)
                        Text("Indian Rupee (₹) • Default for all trips", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Details", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }

                if (showCurrencyInfoDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showCurrencyInfoDialog = false },
                        title = { Text("Currency Settings") },
                        text = {
                            Text("TripMate defaults to Indian Rupee (₹) for all itineraries, activities, stays, and expenses — both for domestic trips within India and international trips abroad.")
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = { showCurrencyInfoDialog = false }) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = Dimens.md)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
