package com.example.tripmate.ui.screens.profile

import android.widget.Toast
import com.example.tripmate.ui.theme.PrimaryOrange
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripmate.model.TravelStat
import com.example.tripmate.model.TripPlan
import com.example.tripmate.ui.components.BottomNavTab
import com.example.tripmate.ui.components.TripMateBottomNav
import com.example.tripmate.ui.theme.Dimens
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onExploreClick: () -> Unit,
    onMyTripsClick: () -> Unit,
    onAssistantClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onTripClick: (TripPlan) -> Unit = {},
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel()
) {
    val displayName by viewModel.displayName.collectAsState()
    val email by viewModel.email.collectAsState()
    val tripHistory by viewModel.tripHistory.collectAsState()
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    val distinctDestinations = remember(tripHistory) {
        tripHistory.map { it.destination.trim().lowercase() }.distinct().size
    }
    val totalPlannedBudget = remember(tripHistory) {
        tripHistory.sumOf { it.budget }
    }
    val stats = remember(tripHistory, distinctDestinations, totalPlannedBudget) {
        val budgetLabel = if (totalPlannedBudget >= 1000) "₹${totalPlannedBudget / 1000}k" else "₹$totalPlannedBudget"
        listOf(
            TravelStat(value = "${tripHistory.size}", label = "Trips Planned"),
            TravelStat(value = "$distinctDestinations", label = "Destinations"),
            TravelStat(value = budgetLabel, label = "Total Budget", isTertiary = true)
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ProfileTopBar(
                onNotificationsClick = {
                    coroutineScope.launch { snackbarHostState.showSnackbar("You're all caught up — no new notifications") }
                }
            )
        },
        bottomBar = {
            com.example.tripmate.ui.components.TripMateBottomNav(
                selectedTab = BottomNavTab.PROFILE,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomNavTab.EXPLORE -> onExploreClick()
                        BottomNavTab.MY_TRIPS -> onMyTripsClick()
                        BottomNavTab.ASSISTANT -> onAssistantClick()
                        BottomNavTab.PROFILE -> { /* already here */ }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimens.marginMobile,
                end = Dimens.marginMobile,
                top = Dimens.lg,
                bottom = innerPadding.calculateBottomPadding() + Dimens.lg
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.lg)
        ) {
            item {
                ProfileHero(
                    displayName = displayName,
                    email = email,
                    onEditNameClick = { showEditNameDialog = true }
                )
            }
            item { TravelStatsRow(stats) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                    Text(
                        text = "My Trips",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (tripHistory.isEmpty()) {
                        NoTripsYetCard()
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.md)) {
                            items(tripHistory) { trip ->
                                SavedTripCard(trip = trip, onClick = { onTripClick(trip) })
                            }
                        }
                    }
                }
            }
            item {
                val sharedTrips by viewModel.sharedTrips.collectAsState()
                var invitingTrip by remember { mutableStateOf<TripPlan?>(null) }
                var showTripSelector by remember { mutableStateOf(false) }

                Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Travel Groups & Shared Trips",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (sharedTrips.isNotEmpty()) {
                            Text(
                                text = "${sharedTrips.size} active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (sharedTrips.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Dimens.radiusCard),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(Dimens.lg), verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = "Co-Plan Group Trips",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = Dimens.xs)
                                    )
                                }
                                Text(
                                    text = "Invite friends to collaborate on itineraries, vote on activities together, and split group expenses across multiple accounts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (tripHistory.isNotEmpty()) {
                                    androidx.compose.material3.TextButton(
                                        onClick = {
                                            if (tripHistory.size == 1) {
                                                invitingTrip = tripHistory.first()
                                            } else {
                                                showTripSelector = true
                                            }
                                        },
                                        modifier = Modifier.padding(top = Dimens.xs)
                                    ) {
                                        Text("+ Invite Friends to a Trip")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.md)) {
                            items(sharedTrips) { trip ->
                                Card(
                                    onClick = { onTripClick(trip) },
                                    modifier = Modifier.width(260.dp),
                                    shape = RoundedCornerShape(Dimens.radiusCard),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(Dimens.md), verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = trip.destination,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(Dimens.radiusFull),
                                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "Group",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF10B981),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = trip.dateRangeLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = Dimens.xs),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "👥 ${maxOf(trip.travelerCount, trip.membersCount)} Travelers",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            androidx.compose.material3.TextButton(
                                                onClick = { invitingTrip = trip },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                            ) {
                                                Text("+ Invite", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showTripSelector) {
                    AlertDialog(
                        onDismissRequest = { showTripSelector = false },
                        title = {
                            Text("Select Trip to Invite Friends", fontWeight = FontWeight.Bold)
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                                Text(
                                    "Select which trip you'd like to collaborate and vote on:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                                    tripHistory.forEach { trip ->
                                        Card(
                                            onClick = {
                                                showTripSelector = false
                                                invitingTrip = trip
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(Dimens.radiusMd),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(Dimens.md),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(trip.destination, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    Text(trip.dateRangeLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = PrimaryOrange)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showTripSelector = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                invitingTrip?.let { trip ->
                    LaunchedEffect(trip) {
                        viewModel.syncTripToCloud(trip)
                    }
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val effectiveTripId = trip.supabaseTripId ?: trip.id
                    val inviteLink = "https://tripmate.app/join/$effectiveTripId"
                    val shareMsg = "Join my ${trip.destination} trip on TripMate! 🌴✈️\n\nClick the link to view the itinerary, vote on activities, and split expenses:\n$inviteLink\n\n(Or enter code: $effectiveTripId)"

                    var emailInput by remember { mutableStateOf("") }
                    var isInviting by remember { mutableStateOf(false) }

                    AlertDialog(
                        onDismissRequest = { invitingTrip = null },
                        title = { Text("Invite to ${trip.destination}", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                                Text(
                                    "Shareable Trip Link",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(Dimens.radiusMd),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = Dimens.sm, vertical = Dimens.xs),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = inviteLink,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(inviteLink))
                                                android.widget.Toast.makeText(context, "Link copied!", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ContentCopy,
                                                contentDescription = "Copy Link",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
                                ) {
                                    androidx.compose.material3.Button(
                                        onClick = {
                                            val sendIntent = android.content.Intent().apply {
                                                action = android.content.Intent.ACTION_SEND
                                                putExtra(android.content.Intent.EXTRA_TEXT, shareMsg)
                                                type = "text/plain"
                                            }
                                            val shareIntent = android.content.Intent.createChooser(sendIntent, "Invite friends to ${trip.destination}")
                                            context.startActivity(shareIntent)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(Dimens.radiusFull)
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Share Link", style = MaterialTheme.typography.labelSmall)
                                    }

                                    androidx.compose.material3.OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(trip.supabaseTripId ?: trip.id))
                                            android.widget.Toast.makeText(context, "Trip code copied!", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(Dimens.radiusFull)
                                    ) {
                                        Text("Copy Code", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.xs))

                                Text(
                                    "Or Invite via Email",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text("Companion Email") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            androidx.compose.material3.Button(
                                onClick = {
                                    if (emailInput.isNotBlank() && !isInviting) {
                                        isInviting = true
                                        viewModel.inviteMember(trip, emailInput) { success, errorMsg ->
                                            isInviting = false
                                            invitingTrip = null
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    if (success) "Invited $emailInput to ${trip.destination}!"
                                                    else errorMsg ?: "Failed to invite companion"
                                                )
                                            }
                                        }
                                    }
                                },
                                enabled = emailInput.isNotBlank() && !isInviting
                            ) {
                                Text(if (isInviting) "Inviting…" else "Send Invite")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { invitingTrip = null }) { Text("Close") }
                        }
                    )
                }
            }
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Dimens.sm),
                    modifier = Modifier.padding(top = Dimens.md)
                ) {
                    SettingsRow(icon = Icons.Filled.Settings, label = "Account Settings", onClick = onSettingsClick)
                    SettingsRow(icon = Icons.AutoMirrored.Filled.HelpCenter, label = "Help & Support", onClick = onHelpClick)
                    Card(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.md),
                        shape = RoundedCornerShape(Dimens.radiusMd),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(Dimens.md),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                text = "Logout",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = Dimens.sm)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditNameDialog) {
        var nameInput by remember { mutableStateOf(displayName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit your name") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateDisplayName(nameInput)
                    showEditNameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log out?") },
            text = { Text("You'll need to sign in again to access your trips.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout(onLoggedOut)
                }) { Text("Log Out", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProfileTopBar(onNotificationsClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Dimens.marginMobile, vertical = Dimens.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "TripMate",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        IconButton(onClick = onNotificationsClick) {
            Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileHero(displayName: String, email: String, onEditNameClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(2.dp, MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Dimens.md)) {
            Text(text = displayName, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            IconButton(onClick = onEditNameClick, modifier = Modifier.size(28.dp).padding(start = 4.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit name", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
        if (email.isNotBlank()) {
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.xs)
            )
        }
    }
}

@Composable
private fun TravelStatsRow(stats: List<TravelStat>, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
        stats.forEach { stat ->
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(Dimens.radiusMd),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Column(modifier = Modifier.padding(Dimens.md), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stat.value,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (stat.isTertiary) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stat.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedTripCard(
    trip: TripPlan,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.width(180.dp).height(140.dp),
        shape = RoundedCornerShape(Dimens.radiusMd),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(Dimens.md), verticalArrangement = Arrangement.Bottom) {
            Text(text = trip.destination, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                text = "${trip.days.size} Days \u2022 ${trip.travelerCount} Traveler${if (trip.travelerCount == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun NoTripsYetCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusMd),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Text(
            text = "Trips you generate will show up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(Dimens.lg)
        )
    }
}


@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusMd),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = Dimens.sm))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
