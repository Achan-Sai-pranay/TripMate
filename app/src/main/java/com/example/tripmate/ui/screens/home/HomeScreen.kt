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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.tripmate.model.UpcomingTrip
import com.example.tripmate.ui.components.BottomNavTab
import com.example.tripmate.ui.components.TripPilotBottomNav
import com.example.tripmate.ui.theme.Dimens

private const val USER_AVATAR_URL =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuCuNVTDMRg1r_UHptkk5G2653Tt632Ge0hBDx5WfDCK77d4H7xRbPsUBdcExMjyaKT6ymf202U63-1FrpoubqPWWKttheWHIUqdvzKHMV9dCaNSgDxDCp_ZLq_KXrCDpkhiIiFaquLfX51ozRhE4SCDpzlisKaKE7Pkat9ezhwAzykRq89Fma3YQ_GHDT9_3x37Fbcwalnzea6NZ6rbXGH5VC3NTLtV_ao4MwxGI-XkHUUfXcxX60ryWA"

private const val UPCOMING_TRIP_IMAGE_URL =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuCkulXU_CfFaaokxWBMQ23aLl5yf3r6ANLhI9_M-1mPlkOO_hg7Ux7tXZ3gt70mmc-NwWFd0Qya02v3jgCcFkKe8G_JsQ3yspXOyoKIcpa-MUs58TZThGalWQMhU1wSA-Hwcq17UKPlKQZysoR9aDqUJDyoCNBU6gtMcqYrV8TcbPIGbQIZwHJFKSpGaHICN2DgNwR21TljPZUPKcUxnz0xRmpI5wKegTwFQ4PIherufeVay1pwuwrhAQ"

@Composable
fun HomeScreen(
    onPlanNewTripClick: () -> Unit,
    onUpcomingTripClick: () -> Unit,
    onMyTripsClick: () -> Unit,
    onAssistantClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val upcomingTrip = remember {
        UpcomingTrip(
            destination = "Hyderabad",
            dateRange = "12-15 September",
            travelerCount = 4,
            imageUrl = UPCOMING_TRIP_IMAGE_URL
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            TripPilotBottomNav(
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
                top = Dimens.xl,
                bottom = innerPadding.calculateBottomPadding() + Dimens.lg
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.xl)
        ) {
            item {
                HomeHeader(userAvatarUrl = USER_AVATAR_URL)
            }

            item {
                HomeSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onFilterClick = { /* filters not in this export */ }
                )
            }

            item {
                PlanNewTripCard(onClick = onPlanNewTripClick)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                    SectionHeader(
                        title = "Upcoming Trip",
                        actionLabel = "View all",
                        onActionClick = onUpcomingTripClick
                    )
                    UpcomingTripCard(
                        destination = upcomingTrip.destination,
                        dateRange = upcomingTrip.dateRange,
                        travelerCount = upcomingTrip.travelerCount,
                        imageUrl = upcomingTrip.imageUrl,
                        onViewTripClick = onUpcomingTripClick
                    )
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
                            onClick = { /* not in this export */ },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            label = "Group Voting",
                            icon = Icons.Filled.HowToVote,
                            onClick = { /* not in this export */ },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    QuickActionCard(
                        label = "Travel Stats",
                        icon = Icons.Filled.BarChart,
                        onClick = { /* not in this export */ },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
