package com.example.tripmate.ui.screens.itinerary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripmate.model.ItineraryDay
import com.example.tripmate.model.ItineraryItem
import com.example.tripmate.model.ItineraryTab
import com.example.tripmate.model.TripSummary
import com.example.tripmate.ui.components.BottomNavTab
import com.example.tripmate.ui.components.TripPilotBottomNav
import com.example.tripmate.ui.shared.TripPlanViewModel
import com.example.tripmate.ui.theme.Dimens

@Composable
fun TripItineraryScreen(
    tripPlanViewModel: TripPlanViewModel,
    onExploreClick: () -> Unit,
    onPlanNewTripClick: () -> Unit,
    onAssistantClick: () -> Unit,
    onProfileClick: () -> Unit,
    onOpenExpenses: (tripId: String) -> Unit,
    modifier: Modifier = Modifier,
    aiViewModel: ItineraryAiViewModel = viewModel()
) {
    val tripPlan by tripPlanViewModel.tripPlan.collectAsState()
    val isGenerating by tripPlanViewModel.isGenerating.collectAsState()
    val planError by tripPlanViewModel.errorMessage.collectAsState()
    val tripRequest by tripPlanViewModel.request.collectAsState()

    val isReplanning by aiViewModel.isReplanning.collectAsState()
    val replacingItemKey by aiViewModel.replacingItemKey.collectAsState()
    val aiError by aiViewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(planError) { planError?.let { snackbarHostState.showSnackbar(it); tripPlanViewModel.dismissError() } }
    LaunchedEffect(aiError) { aiError?.let { snackbarHostState.showSnackbar(it); aiViewModel.dismissError() } }

    var selectedViewTab by remember { mutableStateOf(ItineraryTab.ITINERARY) }
    var currentDayIndex by remember { mutableStateOf(0) }
    var editingItem by remember { mutableStateOf<Pair<Int, ItineraryItem>?>(null) }

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
            val plan = tripPlan
            val currentDay = plan?.days?.getOrNull(currentDayIndex)
            if (plan != null && currentDay != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        aiViewModel.replanDay(
                            destination = plan.destination,
                            dayLabel = "Day ${currentDay.dayNumber} (${currentDay.dateLabel})",
                            currentItems = currentDay.items
                        ) { newItems ->
                            tripPlanViewModel.updateDay(currentDayIndex, newItems)
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
        }
    ) { innerPadding ->
        when {
            isGenerating -> GeneratingTripState(modifier = Modifier.padding(innerPadding))
            tripPlan == null -> EmptyTripState(
                onPlanTripClick = onPlanNewTripClick,
                modifier = Modifier.padding(innerPadding)
            )
            else -> {
                val plan = tripPlan!!
                val currentDay = plan.days.getOrNull(currentDayIndex) ?: plan.days.first()

                val heroImages = remember(plan) {
                    plan.days.flatMap { it.items }.mapNotNull { it.imageUrl }.distinct()
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Dimens.marginMobile,
                        end = Dimens.marginMobile,
                        top = innerPadding.calculateTopPadding() + Dimens.md,
                        bottom = innerPadding.calculateBottomPadding() + Dimens.xxl
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.lg)
                ) {
                    item {
                        TripSummaryCard(
                            trip = TripSummary(
                                destination = plan.destination,
                                dateRange = plan.dateRangeLabel,
                                travelerCount = plan.travelerCount,
                                healthScore = plan.healthScore
                            ),
                            heroImages = heroImages
                        )
                    }
                    // Group expenses button — only shown when a Supabase trip row exists
                    plan.supabaseTripId?.let { tripId ->
                        item {
                            OutlinedButton(
                                onClick = { onOpenExpenses(tripId) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(Dimens.radiusFull)
                            ) {
                                Icon(
                                    Icons.Filled.Groups,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    " Manage Group Expenses",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(start = Dimens.xs)
                                )
                            }
                        }
                    }
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
                            onPreviousDay = { if (currentDayIndex > 0) currentDayIndex-- },
                            onNextDay = { if (currentDayIndex < plan.days.lastIndex) currentDayIndex++ }
                        )
                    }
                    when (selectedViewTab) {
                        ItineraryTab.ITINERARY -> {
                            itemsIndexed(currentDay.items, key = { _, item -> item.time + item.title }) { index, item ->
                                val isReplacing = (item.time + item.title) == replacingItemKey
                                TimelineItemRow(
                                    item = item,
                                    isLastItem = index == currentDay.items.lastIndex,
                                    isReplacing = isReplacing,
                                    onEditClick = { editingItem = index to item },
                                    onReplaceClick = {
                                        aiViewModel.replaceItem(
                                            destination = plan.destination,
                                            item = item
                                        ) { newItem ->
                                            val newItems = currentDay.items.toMutableList().also { it[index] = newItem }
                                            tripPlanViewModel.updateDay(currentDayIndex, newItems)
                                        }
                                    },
                                    onDuplicateClick = {
                                        val duplicate = item.copy()
                                        val newItems = currentDay.items.toMutableList().apply { add(index + 1, duplicate) }
                                        tripPlanViewModel.updateDay(currentDayIndex, newItems)
                                    },
                                    onDeleteClick = {
                                        val newItems = currentDay.items.toMutableList().apply { removeAt(index) }
                                        tripPlanViewModel.updateDay(currentDayIndex, newItems)
                                    }
                                )
                            }
                        }
                        ItineraryTab.BUDGET -> {
                            item {
                                BudgetBreakdownView(
                                    days = plan.days,
                                    totalBudget = tripRequest.budget,
                                    modifier = Modifier.padding(top = Dimens.sm)
                                )
                            }
                        }
                        ItineraryTab.MAP -> {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(240.dp).padding(top = Dimens.sm),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Map view isn't available yet — needs Google Maps integration.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    item {
                        DaySelector(
                            dayNumber = currentDay.dayNumber,
                            dateLabel = currentDay.dateLabel,
                            onPreviousDay = { if (currentDayIndex > 0) currentDayIndex-- },
                            onNextDay = { if (currentDayIndex < plan.days.lastIndex) currentDayIndex++ },
                            modifier = Modifier.padding(top = Dimens.md, bottom = 80.dp) // extra bottom padding for FAB/nav
                        )
                    }
                }
                
                editingItem?.let { (index, item) ->
                    EditItineraryItemDialog(
                        item = item,
                        onDismiss = { editingItem = null },
                        onSave = { updated ->
                            val newItems = currentDay.items.toMutableList().also { it[index] = updated }
                            tripPlanViewModel.updateDay(currentDayIndex, newItems)
                            editingItem = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneratingTripState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.marginMobile, vertical = Dimens.lg)
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = Dimens.md)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "TripPilot AI is curating your personalized itinerary…",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = Dimens.sm)
            )
        }
        com.example.tripmate.ui.components.ItinerarySkeletonLoader(count = 5)
    }
}

@Composable
private fun EmptyTripState(onPlanTripClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(Dimens.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No trip planned yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Start from Explore to plan your first AI itinerary.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = Dimens.xs)
        )
        Button(
            onClick = onPlanTripClick,
            modifier = Modifier.padding(top = Dimens.lg)
        ) {
            Text("Plan a Trip")
        }
    }
}
