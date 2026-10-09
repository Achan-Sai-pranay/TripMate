package com.example.tripmate.ui.screens.itinerary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.tripmate.data.WikipediaImageService
import com.example.tripmate.model.ItineraryDay
import com.example.tripmate.model.ItineraryItem
import com.example.tripmate.model.ItineraryTab
import com.example.tripmate.model.TripSummary
import com.example.tripmate.ui.components.BottomNavTab
import com.example.tripmate.ui.components.TripMateBottomNav
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
    onMyTripsClick: () -> Unit = {},
    onAskAiAboutTrip: (tripId: String) -> Unit = {},
    aiViewModel: ItineraryAiViewModel = viewModel()
) {
    val tripPlan by tripPlanViewModel.tripPlan.collectAsState()
    val isGenerating by tripPlanViewModel.isGenerating.collectAsState()
    val planError by tripPlanViewModel.errorMessage.collectAsState()
    val tripRequest by tripPlanViewModel.request.collectAsState()
    val isResolvingPlaces by tripPlanViewModel.isResolvingPlaces.collectAsState()

    val isReplanning by aiViewModel.isReplanning.collectAsState()
    val replacingItemKey by aiViewModel.replacingItemKey.collectAsState()
    val aiError by aiViewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(planError) { planError?.let { snackbarHostState.showSnackbar(it); tripPlanViewModel.dismissError() } }
    LaunchedEffect(aiError) { aiError?.let { snackbarHostState.showSnackbar(it); aiViewModel.dismissError() } }

    val tripMembers by tripPlanViewModel.tripMembers.collectAsState()
    var showInviteSheet by remember { mutableStateOf(false) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    var selectedViewTab by remember { mutableStateOf(ItineraryTab.ITINERARY) }
    var currentDayIndex by remember { mutableStateOf(0) }
    var editingItem by remember { mutableStateOf<Pair<Int, ItineraryItem>?>(null) }
    // Shared by the itinerary list and the map: the currently highlighted place (pin key)
    var selectedPinKey by remember { mutableStateOf<String?>(null) }
    var focusToken by remember { mutableStateOf(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            com.example.tripmate.ui.components.TripMateBottomNav(
                selectedTab = BottomNavTab.MY_TRIPS,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomNavTab.EXPLORE -> onExploreClick()
                        BottomNavTab.MY_TRIPS -> onMyTripsClick()
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
            isGenerating -> GeneratingTripState(
                destination = tripRequest.destination,
                modifier = Modifier.padding(innerPadding)
            )
            tripPlan == null -> EmptyTripState(
                onPlanTripClick = onPlanNewTripClick,
                modifier = Modifier.padding(innerPadding)
            )
            else -> {
                val plan = tripPlan!!
                val currentDay = plan.days.getOrNull(currentDayIndex) ?: plan.days.first()

                LaunchedEffect(plan.destination) {
                    if (plan.days.any { day -> day.items.any { !it.hasCoordinates } }) {
                        tripPlanViewModel.resolveMissingCoordinates()
                    }
                }

                LaunchedEffect(plan.supabaseTripId) {
                    tripPlanViewModel.loadTripMembers()
                    tripPlanViewModel.syncExpensesFromCloud()
                }

                LaunchedEffect(selectedViewTab) {
                    if (selectedViewTab == ItineraryTab.BUDGET) {
                        tripPlanViewModel.syncExpensesFromCloud()
                    }
                }

                val heroImages = remember(plan) {
                    plan.days.flatMap { it.items }.mapNotNull { it.imageUrl }.distinct()
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Dimens.marginMobile,
                        end = Dimens.marginMobile,
                        top = innerPadding.calculateTopPadding() + Dimens.md,
                        bottom = innerPadding.calculateBottomPadding() + 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.lg)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.TextButton(
                                onClick = onMyTripsClick,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to All Trips",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "All Trips",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            androidx.compose.material3.FilledTonalButton(
                                onClick = { onAskAiAboutTrip(plan.id) },
                                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(Dimens.radiusFull)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Ask AI",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    item {
                        TripSummaryCard(
                            trip = TripSummary(
                                destination = plan.destination,
                                dateRange = plan.dateRangeLabel,
                                travelerCount = plan.travelerCount
                            ),
                            heroImages = heroImages
                        )
                    }
                    // Trip collaboration & companions section: Member avatars, invite link & group expenses
                    item {
                        TripCollaborationCard(
                            plan = plan,
                            members = tripMembers,
                            onInviteClick = { showInviteSheet = true },
                            onManageExpensesClick = {
                                coroutineScope.launch {
                                    val tripId = plan.supabaseTripId ?: tripPlanViewModel.ensureTripSyncedToCloud()
                                    if (tripId != null) {
                                        onOpenExpenses(tripId)
                                    } else {
                                        snackbarHostState.showSnackbar("Please sign in to manage group expenses.")
                                    }
                                }
                            }
                        )
                    }
                    item {
                        ItineraryViewTabs(
                            selectedTab = selectedViewTab,
                            onTabSelected = { selectedViewTab = it }
                        )
                    }
                    when (selectedViewTab) {
                        ItineraryTab.ITINERARY -> {
                            item {
                                DaySelector(
                                    dayNumber = currentDay.dayNumber,
                                    dateLabel = currentDay.dateLabel,
                                    onPreviousDay = { if (currentDayIndex > 0) currentDayIndex-- },
                                    onNextDay = { if (currentDayIndex < plan.days.lastIndex) currentDayIndex++ },
                                    modifier = Modifier.padding(vertical = Dimens.xs)
                                )
                            }
                            item {
                                ItineraryMapCard(
                                    destination = plan.destination,
                                    day = currentDay,
                                    selectedKey = selectedPinKey,
                                    focusToken = focusToken,
                                    isResolving = isResolvingPlaces,
                                    onPinSelected = { selectedPinKey = it.key; focusToken++ }
                                )
                            }
                            itemsIndexed(
                                items = currentDay.items,
                                key = { _, item -> item.id }
                            ) { index, item ->
                                val isReplacing = (item.time + item.title) == replacingItemKey
                                val pinKey = currentDay.pinKey(index)
                                val targetItemId = item.id
                                val order = currentDay.placeOrder(index)

                                val prevBlock = if (index > 0) currentDay.items[index - 1].resolvedTimeBlock else null
                                val currentBlock = item.resolvedTimeBlock
                                if (index == 0 || prevBlock != currentBlock) {
                                    TimeBlockHeader(
                                        block = currentBlock,
                                        modifier = Modifier.padding(top = if (index == 0) Dimens.xs else Dimens.sm, bottom = Dimens.xs)
                                    )
                                }

                                TimelineItemRow(
                                    item = item,
                                    order = order,
                                    dayNumber = currentDay.dayNumber,
                                    nextItem = currentDay.items.getOrNull(index + 1),
                                    destination = plan.destination,
                                    isSelected = pinKey != null && pinKey == selectedPinKey,
                                    onClick = pinKey?.let { key -> { selectedPinKey = key; focusToken++ } },
                                    isLastItem = index == currentDay.items.lastIndex,
                                    isReplacing = isReplacing,
                                    onVoteClick = { isUpvote ->
                                        tripPlanViewModel.castVote(currentDayIndex, item.id, if (isUpvote) "UP" else "DOWN")
                                    },
                                    onEditClick = { editingItem = index to item },
                                    onReplaceClick = {
                                        aiViewModel.replaceItem(
                                            destination = plan.destination,
                                            item = item
                                        ) { newItem ->
                                            val latestDay = tripPlanViewModel.tripPlan.value?.days?.getOrNull(currentDayIndex) ?: currentDay
                                            val latestIndex = latestDay.items.indexOfFirst { it.id == targetItemId }
                                            if (latestIndex >= 0) {
                                                val newItems = latestDay.items.toMutableList().also {
                                                    it[latestIndex] = newItem.copy(id = targetItemId)
                                                }
                                                tripPlanViewModel.updateDay(currentDayIndex, newItems)
                                            }
                                        }
                                    },
                                    onDuplicateClick = {
                                        val duplicate = item.copy(id = java.util.UUID.randomUUID().toString())
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
                                    plan = plan,
                                    onUpdateBudget = { newBudget -> tripPlanViewModel.updateBudget(newBudget) },
                                    onAddExpense = { entry -> tripPlanViewModel.addExpense(entry) },
                                    onUpdateExpense = { entry -> tripPlanViewModel.updateExpense(entry) },
                                    onDeleteExpense = { id -> tripPlanViewModel.deleteExpense(id) },
                                    onOpenExpenses = onOpenExpenses,
                                    onReplanCheaper = {
                                        val currentDay = plan.days.getOrNull(currentDayIndex) ?: plan.days.firstOrNull()
                                        if (currentDay != null) {
                                            aiViewModel.replanDay(
                                                destination = plan.destination,
                                                dayLabel = "Day ${currentDay.dayNumber} (${currentDay.dateLabel}) - low budget alternatives",
                                                currentItems = currentDay.items
                                            ) { newItems ->
                                                tripPlanViewModel.updateDay(currentDayIndex, newItems)
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(top = Dimens.sm)
                                )
                            }
                        }
                        ItineraryTab.MAP -> {
                            item {
                                InteractiveMapTab(
                                    destination = plan.destination,
                                    days = plan.days,
                                    selectedKey = selectedPinKey,
                                    focusToken = focusToken,
                                    isResolving = isResolvingPlaces,
                                    onSelect = { selectedPinKey = it; focusToken++ },
                                    modifier = Modifier.padding(top = Dimens.sm)
                                )
                            }
                        }
                        ItineraryTab.STAYS -> {
                            item {
                                StaysSectionView(
                                    stays = plan.stays,
                                    modifier = Modifier.padding(top = Dimens.sm)
                                )
                            }
                        }
                        ItineraryTab.DINING -> {
                            item {
                                DiningSectionView(
                                    dining = plan.dining,
                                    modifier = Modifier.padding(top = Dimens.sm)
                                )
                            }
                        }
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

                if (showInviteSheet) {
                    TripInviteSheet(
                        tripPlanViewModel = tripPlanViewModel,
                        destination = plan.destination,
                        onDismiss = { showInviteSheet = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneratingTripState(
    destination: String,
    modifier: Modifier = Modifier
) {
    var heroImageUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(destination) {
        if (destination.isNotBlank()) {
            heroImageUrl = WikipediaImageService.imageForDestination(destination)
        }
    }

    val subtitles = remember {
        listOf(
            "Finding hidden gems…",
            "Mapping the best routes…",
            "Checking opening hours…",
            "Curating local delicacies…",
            "Personalizing your schedule…"
        )
    }
    var subtitleIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2500)
            subtitleIndex = (subtitleIndex + 1) % subtitles.size
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.marginMobile, vertical = Dimens.md)
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp),
            shape = RoundedCornerShape(Dimens.radiusCard),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp)) {
                val img = heroImageUrl ?: WikipediaImageService.FALLBACK_IMAGE_URL
                AsyncImage(
                    model = img,
                    contentDescription = destination,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.65f),
                                    Color.Black.copy(alpha = 0.90f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(Dimens.lg)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = Dimens.xs)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(Dimens.xs))
                        Text(
                            text = "AI ITINERARY GENERATOR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = if (destination.isNotBlank()) "Crafting your trip to $destination" else "Crafting your personalized trip",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    AnimatedContent(
                        targetState = subtitles[subtitleIndex],
                        transitionSpec = {
                            fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                        },
                        label = "subtitleCrossfade"
                    ) { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimens.lg))
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
