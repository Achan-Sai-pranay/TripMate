package com.example.tripmate.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.os.Build
import com.example.tripmate.util.TripNotificationHelper
import com.example.tripmate.data.AuthRepository
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import com.example.tripmate.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val userPrefs = remember { com.example.tripmate.data.UserPreferencesRepository(context.applicationContext) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val darkModePref by userPrefs.darkModeFlow.collectAsState(initial = "LIGHT")
    val pushNotifications by userPrefs.pushNotificationsFlow.collectAsState(initial = true)
    val emailNotifications by userPrefs.emailNotificationsFlow.collectAsState(initial = false)
    val isDarkMode = darkModePref == "DARK"
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        coroutineScope.launch {
            userPrefs.setPushNotifications(isGranted)
            if (isGranted) {
                TripNotificationHelper.initChannels(context)
                TripNotificationHelper.postNotification(
                    context,
                    title = "TripMate Alerts Active 🎉",
                    message = "You'll now receive timely updates for trip schedules, votes, and expenses."
                )
                snackbarHostState.showSnackbar("Push notifications enabled")
            } else {
                snackbarHostState.showSnackbar("Notification permission was denied")
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                .padding(Dimens.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.xl)
        ) {
            SettingSection(title = "Appearance") {
                SettingToggleRow(
                    label = "Dark Mode",
                    description = "Enable modern dark theme",
                    checked = isDarkMode,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            userPrefs.setDarkMode(if (enabled) "DARK" else "LIGHT")
                        }
                    }
                )
            }
            
            SettingSection(title = "Notifications") {
                SettingToggleRow(
                    label = "Push Notifications",
                    description = "Get notified about your trip updates and votes",
                    checked = pushNotifications,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val isGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (isGranted) {
                                        userPrefs.setPushNotifications(true)
                                        TripNotificationHelper.initChannels(context)
                                        TripNotificationHelper.postNotification(
                                            context,
                                            title = "TripMate Alerts Active 🎉",
                                            message = "You'll now receive timely updates for trip schedules, votes, and expenses."
                                        )
                                        snackbarHostState.showSnackbar("Push notifications enabled")
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    userPrefs.setPushNotifications(true)
                                    TripNotificationHelper.initChannels(context)
                                    TripNotificationHelper.postNotification(
                                        context,
                                        title = "TripMate Alerts Active 🎉",
                                        message = "You'll now receive timely updates for trip schedules, votes, and expenses."
                                    )
                                    snackbarHostState.showSnackbar("Push notifications enabled")
                                }
                            } else {
                                userPrefs.setPushNotifications(false)
                                TripNotificationHelper.cancelAll(context)
                                snackbarHostState.showSnackbar("Push notifications turned off")
                            }
                        }
                    }
                )
                SettingToggleRow(
                    label = "Email Updates",
                    description = "Receive weekly travel tips and itinerary invites",
                    checked = emailNotifications,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            userPrefs.setEmailNotifications(enabled)
                            val userEmail = AuthRepository().currentUserEmail() ?: "your registered email"
                            snackbarHostState.showSnackbar(
                                if (enabled) "Email updates enabled. Updates will be sent to $userEmail."
                                else "Email updates paused."
                            )
                        }
                    }
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
