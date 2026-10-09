package com.example.tripmate.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripmate.ui.components.BottomNavTab
import com.example.tripmate.ui.components.TripMateBottomNav
import com.example.tripmate.ui.shared.TripPlanViewModel
import com.example.tripmate.ui.theme.Dimens


@Composable
fun HomeScreen(
    tripPlanViewModel: TripPlanViewModel,
    onPlanNewTripClick: () -> Unit,
    onUpcomingTripClick: () -> Unit,
    onMyTripsClick: () -> Unit,
    onAssistantClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSearchDestination: (String) -> Unit,
    onQuickTripLength: (Long, Long) -> Unit,
    onTravelStatsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val tripPlan by tripPlanViewModel.tripPlan.collectAsState()
    val budgetGems by viewModel.budgetGems.collectAsState()
    val isLoadingGems by viewModel.isLoadingGems.collectAsState()
    val gemsError by viewModel.errorMessage.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showBudgetGemsSheet by remember { mutableStateOf(false) }
    var showJoinTripDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        tripPlanViewModel.resetAndLoadForCurrentUser()
    }

    LaunchedEffect(gemsError) {
        gemsError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    val authRepo = remember { com.example.tripmate.data.AuthRepository() }
    val profileRepo = remember { com.example.tripmate.data.ProfileRepository() }
    val currentUserId = remember { authRepo.currentUserId() }
    val currentUserName = remember {
        authRepo.currentUserName() ?: "Traveler"
    }
    var userAvatarUrl by remember {
        mutableStateOf(com.example.tripmate.data.ProfileRepository.getCachedAvatarUrl() ?: authRepo.currentUserAvatarUrl().orEmpty())
    }

    LaunchedEffect(currentUserId) {
        if (!currentUserId.isNullOrBlank()) {
            val profile = profileRepo.fetchProfile(currentUserId)
            val fetchedAvatar = profile?.avatarUrl
            if (!fetchedAvatar.isNullOrBlank()) {
                userAvatarUrl = fetchedAvatar
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            com.example.tripmate.ui.components.TripMateBottomNav(
                selectedTab = BottomNavTab.EXPLORE,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomNavTab.EXPLORE -> { /* already here */ }
                        BottomNavTab.MY_TRIPS -> onMyTripsClick()
                        BottomNavTab.ASSISTANT -> onAssistantClick()
                        BottomNavTab.PROFILE -> onProfileClick()
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
                top = innerPadding.calculateTopPadding() + WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Dimens.xs,
                bottom = innerPadding.calculateBottomPadding() + Dimens.lg
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.xl)
        ) {
            item {
                HomeHeader(
                    userName = currentUserName,
                    userAvatarUrl = userAvatarUrl,
                    onAvatarClick = onProfileClick
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
                    ) {
                        com.example.tripmate.ui.components.SmartDestinationSearchField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onDestinationSelected = { dest ->
                                searchQuery = "${dest.name}, ${dest.country}"
                                onSearchDestination(searchQuery)
                            },
                            placeholder = "Where to? (e.g. Goa, Paris, Manali)",
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            onClick = { showFilterSheet = true },
                            modifier = Modifier.size(54.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = "Filter trip duration",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Quick duration filter chips row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { showFilterSheet = true },
                            shape = RoundedCornerShape(Dimens.radiusFull),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Trip Length",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                val now = System.currentTimeMillis()
                                onQuickTripLength(now, now + 2 * 24 * 60 * 60 * 1000L)
                            },
                            shape = RoundedCornerShape(Dimens.radiusFull),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = "Weekend",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            onClick = {
                                val now = System.currentTimeMillis()
                                onQuickTripLength(now, now + 6 * 24 * 60 * 60 * 1000L)
                            },
                            shape = RoundedCornerShape(Dimens.radiusFull),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = "1 Week",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            onClick = {
                                val now = System.currentTimeMillis()
                                onQuickTripLength(now, now + 13 * 24 * 60 * 60 * 1000L)
                            },
                            shape = RoundedCornerShape(Dimens.radiusFull),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = "2 Weeks",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            item {
                PlanNewTripCard(onClick = onPlanNewTripClick)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                    SectionHeader(
                        title = "Upcoming Trip",
                        actionLabel = if (tripPlan != null) "View all" else null,
                        onActionClick = onMyTripsClick
                    )
                    val plan = tripPlan
                    if (plan == null) {
                        NoUpcomingTripCard(onPlanClick = onPlanNewTripClick)
                    } else {
                        // Photo of THIS trip's destination (cached after the first lookup)
                        val tripImageUrl by androidx.compose.runtime.produceState(
                            initialValue = "",
                            plan.destination
                        ) {
                            value = com.example.tripmate.data.WikipediaImageService.imageForDestination(plan.destination)
                                ?: com.example.tripmate.data.WikipediaImageService.FALLBACK_IMAGE_URL
                        }
                        UpcomingTripCard(
                            destination = plan.destination,
                            dateRange = plan.dateRangeLabel,
                            travelerCount = plan.travelerCount,
                            imageUrl = tripImageUrl,
                            onViewTripClick = onUpcomingTripClick
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                    SectionHeader(title = "Quick Actions")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.md)
                    ) {
                        QuickActionCard(
                            label = "Discover Budget Gems",
                            icon = Icons.Filled.LocalActivity,
                            onClick = {
                                showBudgetGemsSheet = true
                                if (budgetGems.isEmpty()) viewModel.fetchBudgetGems()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            label = "Join a Trip",
                            icon = Icons.Filled.GroupAdd,
                            onClick = { showJoinTripDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    QuickActionCard(
                        label = "Travel Stats",
                        icon = Icons.Filled.BarChart,
                        onClick = onTravelStatsClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        QuickTripLengthSheet(
            onDismiss = { showFilterSheet = false },
            onSelect = { start, end ->
                showFilterSheet = false
                onQuickTripLength(start, end)
            }
        )
    }

    if (showBudgetGemsSheet) {
        BudgetGemsSheet(
            gems = budgetGems,
            isLoading = isLoadingGems,
            onDismiss = { showBudgetGemsSheet = false },
            onPickDestination = { destination ->
                showBudgetGemsSheet = false
                onSearchDestination(destination)
            }
        )
    }

    if (showJoinTripDialog) {
        com.example.tripmate.ui.screens.itinerary.JoinTripDialog(
            tripPlanViewModel = tripPlanViewModel,
            onDismiss = { showJoinTripDialog = false },
            onSuccess = { dest ->
                showJoinTripDialog = false
                onUpcomingTripClick()
            }
        )
    }
}
