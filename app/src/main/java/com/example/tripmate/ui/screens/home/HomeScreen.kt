package com.example.tripmate.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

private const val USER_AVATAR_URL =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuCuNVTDMRg1r_UHptkk5G2653Tt632Ge0hBDx5WfDCK77d4H7xRbPsUBdcExMjyaKT6ymf202U63-1FrpoubqPWWKttheWHIUqdvzKHMV9dCaNSgDxDCp_ZLq_KXrCDpkhiIiFaquLfX51ozRhE4SCDpzlisKaKE7Pkat9ezhwAzykRq89Fma3YQ_GHDT9_3x37Fbcwalnzea6NZ6rbXGH5VC3NTLtV_ao4MwxGI-XkHUUfXcxX60ryWA"

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
    var showGroupVotingInfo by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(gemsError) {
        gemsError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
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
                top = innerPadding.calculateTopPadding() + Dimens.md,
                bottom = innerPadding.calculateBottomPadding() + Dimens.lg
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.xl)
        ) {
            item {
                HomeHeader(
                    userAvatarUrl = USER_AVATAR_URL,
                    onAvatarClick = onProfileClick
                )
            }

            item {
                Column {
                    com.example.tripmate.ui.components.SmartDestinationSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onDestinationSelected = { dest ->
                            searchQuery = "${dest.name}, ${dest.country}"
                            onSearchDestination(searchQuery)
                        },
                        placeholder = "Where to? (e.g. Goa, Paris, Manali)"
                    )
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
                        onActionClick = onUpcomingTripClick
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
                            label = "Group Voting",
                            icon = Icons.Filled.HowToVote,
                            onClick = { showGroupVotingInfo = true },
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

    if (showGroupVotingInfo) {
        AlertDialog(
            onDismissRequest = { showGroupVotingInfo = false },
            title = { Text("Not available yet") },
            text = { Text("Group voting needs multiple people to have accounts and join the same trip — that's not built yet, but it's on the roadmap.") },
            confirmButton = {
                TextButton(onClick = { showGroupVotingInfo = false }) {
                    Text("Got it")
                }
            }
        )
    }
}
