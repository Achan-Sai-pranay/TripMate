package com.example.tripmate.ui.screens.itinerary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripmate.model.ItineraryDay
import com.example.tripmate.model.ItineraryItem
import com.example.tripmate.model.ItineraryTab
import com.example.tripmate.model.TripSummary
import com.example.tripmate.ui.components.BottomNavTab
import com.example.tripmate.ui.components.TripPilotBottomNav
import com.example.tripmate.ui.theme.Dimens

private const val GOLCONDA_FORT_IMAGE_URL =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuDp4HfPx7-KGD0w03zCbM9eUkmTV50mBbUiWwslqiVF8E1P02SzYNpXJALKPj6ZUhxk1jk3LlXuKMx7eqB4AfL54uvSZpyAraZsjxry42wJmVLxPHiPqCn05k4Y47-zeeF-A7BulAJqNV-Hfedn_MZaJlyvL274WBp9WTmqmBiq-A06hMeIvZqDZb4BWdyWA3J5jI-cxkHiJSP_Hmy8p6h3WMQlE7roCDhsAW8O5VRKHv6ynS1ONRNrUw"

@Composable
fun TripItineraryScreen(
    onExploreClick: () -> Unit,
    onAssistantClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    aiViewModel: ItineraryAiViewModel = viewModel()
) {
    val isReplanning by aiViewModel.isReplanning.collectAsState()
    val replacingItemKey by aiViewModel.replacingItemKey.collectAsState()
    val errorMessage by aiViewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            aiViewModel.dismissError()
        }
    }

    val tripSummary = remember {
        TripSummary(
            destination = "Hyderabad Getaway",
            dateRange = "Oct 12 - Oct 15",
            travelerCount = 3,
            healthScore = 87
        )
    }

    var selectedViewTab by remember { mutableStateOf(ItineraryTab.ITINERARY) }

    val days = remember {
        mutableStateListOf(
            ItineraryDay(
                dayNumber = 1,
                dateLabel = "Oct 12",
                items = listOf(
                    ItineraryItem(
                        time = "09:00 AM",
                        title = "Breakfast at Local Cafe",
                        durationLabel = "1h",
                        costLabel = "\u20B9300",
                        whyThis = "Highly rated traditional breakfast, perfectly on route to your first attraction.",
                        icon = Icons.Filled.Restaurant
                    ),
                    ItineraryItem(
                        time = "10:30 AM",
                        title = "Golconda Fort",
                        durationLabel = "2.5h",
                        costLabel = "\u20B9200",
                        whyThis = "Matches History interest. Early visit avoids peak afternoon heat and crowds.",
                        icon = Icons.Filled.Castle,
                        imageUrl = GOLCONDA_FORT_IMAGE_URL
                    ),
                    ItineraryItem(
                        time = "01:00 PM",
                        title = "Biryani Lunch",
                        durationLabel = "1h",
                        costLabel = "\u20B9600",
                        whyThis = "Iconic local cuisine. Located within 15 mins of Golconda Fort for easy transit.",
                        icon = Icons.Filled.LocalDining
                    )
                )
            )
        )
    }

    var currentDayIndex by remember { mutableStateOf(0) }
    val currentDay = days.getOrNull(currentDayIndex) ?: ItineraryDay(1, "Oct 12", emptyList())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            TripPilotBottomNav(
                selectedTab = BottomNavTab.MY_TRIPS,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomNavTab.EXPLORE -> onExploreClick()
                        BottomNavTab.MY_TRIPS -> { /* already here */ }
                        BottomNavTab.ASSISTANT -> onAssistantClick()
                        BottomNavTab.PROFILE -> onProfileClick()
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    aiViewModel.replanDay(
                        destination = tripSummary.destination,
                        dayLabel = "Day ${currentDay.dayNumber} (${currentDay.dateLabel})",
                        currentItems = currentDay.items
                    ) { newItems ->
                        if (currentDayIndex in days.indices) {
                            days[currentDayIndex] = currentDay.copy(items = newItems)
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                if (isReplanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(imageVector = Icons.Filled.AutoFixHigh, contentDescription = null)
                }
                Text(
                    text = if (isReplanning) "Replanning…" else "Replan",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = Dimens.xs)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimens.marginMobile,
                end = Dimens.marginMobile,
                top = Dimens.lg,
                bottom = innerPadding.calculateBottomPadding() + Dimens.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.lg)
        ) {
            item { TripSummaryCard(trip = tripSummary) }

            item {
                ItineraryViewTabs(
                    selectedTab = selectedViewTab,
                    onTabSelected = { selectedViewTab = it }
                )
            }

            item {
                DaySelector(
                    dayNumber = currentDay.dayNumber,
                    dateLabel = currentDay.dateLabel,
                    onPreviousDay = {
                        if (currentDayIndex > 0) currentDayIndex--
                    },
                    onNextDay = {
                        if (currentDayIndex < days.lastIndex) currentDayIndex++
                    }
                )
            }

            itemsIndexed(currentDay.items) { index, item ->
                val isReplacing = (item.time + item.title) == replacingItemKey
                TimelineItemRow(
                    item = item,
                    isLastItem = index == currentDay.items.lastIndex,
                    isReplacing = isReplacing,
                    onEditClick = { /* wired up in the features pass */ },
                    onReplaceClick = {
                        aiViewModel.replaceItem(
                            destination = tripSummary.destination,
                            item = item
                        ) { newItem ->
                            val updatedItems = currentDay.items.toMutableList().also { it[index] = newItem }
                            if (currentDayIndex in days.indices) {
                                days[currentDayIndex] = currentDay.copy(items = updatedItems)
                            }
                        }
                    },
                    onDuplicateClick = {
                        val duplicate = item.copy()
                        val updatedItems = currentDay.items.toMutableList()
                        updatedItems.add(index + 1, duplicate)
                        val updatedDay = currentDay.copy(items = updatedItems)
                        days[currentDayIndex] = updatedDay
                    },
                    onDeleteClick = {
                        val updatedItems = currentDay.items.toMutableList()
                        updatedItems.removeAt(index)
                        val updatedDay = currentDay.copy(items = updatedItems)
                        days[currentDayIndex] = updatedDay
                    }
                )
            }
        }
    }
}
