package com.example.tripmate.ui.screens.itinerary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.Surface
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.tripmate.model.ItineraryDay
import com.example.tripmate.model.ItineraryItem
import com.example.tripmate.model.ItineraryTab
import com.example.tripmate.model.StayOption
import com.example.tripmate.model.DiningOption
import com.example.tripmate.model.TripPlan
import com.example.tripmate.model.BudgetEntry
import com.example.tripmate.model.ExpenseCategory
import com.example.tripmate.model.TripSummary
import com.example.tripmate.ui.theme.Dimens

@Composable
fun TripSummaryCard(
    trip: TripSummary,
    heroImages: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            if (heroImages.isNotEmpty()) {
                var currentImageIndex by remember(heroImages) { mutableStateOf(0) }
                LaunchedEffect(heroImages) {
                    if (heroImages.size > 1) {
                        while (true) {
                            kotlinx.coroutines.delay(4000)
                            currentImageIndex = (currentImageIndex + 1) % heroImages.size
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    androidx.compose.animation.Crossfade(
                        targetState = heroImages[currentImageIndex],
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 800),
                        label = "heroImageCrossfade"
                    ) { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = trip.destination,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        )
                    }
                    // Gradient scrim overlay for high text contrast
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.25f),
                                        androidx.compose.ui.graphics.Color.Transparent,
                                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(Dimens.md)
                    ) {
                        Text(
                            text = trip.destination,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                        Text(
                            text = "${trip.dateRange} • ${trip.travelerCount} Travelers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.padding(Dimens.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "CURRENT TRIP",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = Dimens.xs)
                        )
                    }
                    Text(
                        text = trip.destination,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "${trip.dateRange} • ${trip.travelerCount} Travelers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ItineraryViewTabs(
    selectedTab: ItineraryTab,
    onTabSelected: (ItineraryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
    ) {
        items(ItineraryTab.entries) { tab ->
            val isSelected = tab == selectedTab
            Card(
                onClick = { onTabSelected(tab) },
                shape = RoundedCornerShape(Dimens.radiusFull),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = Dimens.md, vertical = Dimens.sm)
                ) {
                    Icon(
                        imageVector = tabIcon(tab),
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = Dimens.xs)
                    )
                }
            }
        }
    }
}

@Composable
private fun tabIcon(tab: ItineraryTab) = when (tab) {
    ItineraryTab.ITINERARY -> Icons.AutoMirrored.Filled.FormatListBulleted
    ItineraryTab.MAP -> Icons.Filled.Map
    ItineraryTab.STAYS -> Icons.Filled.Hotel
    ItineraryTab.DINING -> Icons.Filled.Restaurant
    ItineraryTab.BUDGET -> Icons.Filled.Payments
}

@Composable
fun DaySelector(
    dayNumber: Int,
    dateLabel: String,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "Day $dayNumber",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = " • $dateLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            DayNavButton(icon = Icons.Filled.ChevronLeft, onClick = onPreviousDay)
            DayNavButton(icon = Icons.Filled.ChevronRight, onClick = onNextDay)
        }
    }
}

@Composable
private fun DayNavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(Dimens.radiusMd))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(Dimens.radiusMd)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TimelineItemRow(
    item: ItineraryItem,
    isLastItem: Boolean,
    isReplacing: Boolean,
    onEditClick: () -> Unit,
    onReplaceClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    destination: String = ""
) {
    // Destination-specific photo: the item's stored image, else a cached/looked-up photo of THIS place.
    // Generic activities (breakfast, free time, check-in...) have no place and keep the plain card.
    val placeName = remember(item.title, item.placeName) {
        com.example.tripmate.data.WikipediaImageService.placeNameFor(item)
    }
    val bgImageUrl by androidx.compose.runtime.produceState<String?>(
        initialValue = item.imageUrl?.takeIf { it.isNotBlank() },
        item.imageUrl, placeName, destination, item.wikipediaTitle
    ) {
        val stored = item.imageUrl?.takeIf { it.isNotBlank() }
        value = stored ?: placeName?.let {
            com.example.tripmate.data.WikipediaImageService.imageForPlace(it, destination, item.wikipediaTitle)
                ?: com.example.tripmate.data.WikipediaImageService.imageForDestination(destination)
                ?: com.example.tripmate.data.WikipediaImageService.FALLBACK_IMAGE_URL
        }
    }

    Row(modifier = modifier.fillMaxWidth()) {
        // Timeline node + connector line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
            if (!isLastItem) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .padding(vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }

        // Activity card with scenic background image & gradient scrim
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(start = Dimens.md, bottom = Dimens.lg)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            shape = RoundedCornerShape(Dimens.radiusLg),
            border = if (isSelected) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background destination/activity photo with shimmer/gradient placeholder
                bgImageUrl?.let { url ->
                    SubcomposeAsyncImage(
                        model = url,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                        loading = {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                        )
                                    )
                            )
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            listOf(Color(0xFF334155), Color(0xFF1E293B))
                                        )
                                    )
                            )
                        }
                    )
                }

                // High contrast dark gradient scrim
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.50f),
                                    Color.Black.copy(alpha = 0.70f),
                                    Color.Black.copy(alpha = 0.92f)
                                )
                            )
                        )
                )

                Column(modifier = Modifier.padding(Dimens.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(Dimens.radiusFull),
                            color = com.example.tripmate.ui.theme.PrimaryOrange
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = Dimens.sm, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.time,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (item.isFixed) {
                                    Icon(
                                        imageVector = Icons.Filled.PushPin,
                                        contentDescription = "Fixed commitment",
                                        tint = Color.White,
                                        modifier = Modifier.padding(start = 4.dp).size(12.dp)
                                    )
                                }
                            }
                        }

                        Box {
                            var menuExpanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "More options",
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                if (!item.isFixed) {
                                    DropdownMenuItem(
                                        text = { Text("Duplicate") },
                                        leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                                        onClick = { menuExpanded = false; onDuplicateClick() }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = { menuExpanded = false; onDeleteClick() }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("This is a fixed commitment", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = { menuExpanded = false },
                                        enabled = false
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = Dimens.xs)
                    )

                    Row(
                        modifier = Modifier.padding(top = Dimens.xs, bottom = Dimens.sm),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                    ) {
                        InfoChip(icon = Icons.Filled.Schedule, label = item.durationLabel, isDark = true)
                        InfoChip(icon = Icons.Filled.Payments, label = item.costLabel, isDark = true)
                    }

                    // Glassmorphic "Why this?" box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.radiusMd))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(Dimens.radiusMd))
                            .padding(Dimens.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Column(modifier = Modifier.padding(start = Dimens.xs)) {
                            Text(
                                text = "Why this?",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = item.whyThis,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Bottom Action Buttons with robust layout (no text clipping)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimens.sm),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(Dimens.radiusMd),
                            color = Color.White.copy(alpha = 0.22f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable { onEditClick() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Edit",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }

                        if (!item.isFixed) {
                            Surface(
                                shape = RoundedCornerShape(Dimens.radiusMd),
                                color = Color.White,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable(enabled = !isReplacing) { onReplaceClick() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isReplacing) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = com.example.tripmate.ui.theme.PrimaryOrange
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.SwapHoriz,
                                            contentDescription = null,
                                            tint = Color(0xFF1E293B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (isReplacing) "Finding..." else "Replace",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isDark: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.radiusSm))
            .background(if (isDark) Color.White.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = Dimens.sm, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDark) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isDark) FontWeight.Medium else FontWeight.Normal,
            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

private fun categoryColor(category: ExpenseCategory): Color = when (category) {
    ExpenseCategory.ACTIVITIES -> Color(0xFF3B82F6) // Blue
    ExpenseCategory.FOOD -> Color(0xFFF97316)       // Orange
    ExpenseCategory.STAY -> Color(0xFF8B5CF6)       // Purple
    ExpenseCategory.TRANSPORT -> Color(0xFF10B981)  // Emerald
    ExpenseCategory.SHOPPING -> Color(0xFFEC4899)   // Pink
    ExpenseCategory.OTHER -> Color(0xFF64748B)      // Slate
}

@Composable
fun BudgetBreakdownView(
    plan: TripPlan,
    onUpdateBudget: (Int) -> Unit,
    onAddExpense: (BudgetEntry) -> Unit,
    onUpdateExpense: (BudgetEntry) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onReplanCheaper: () -> Unit,
    modifier: Modifier = Modifier
) {
    var includeStays by remember { mutableStateOf(true) }
    var includeDining by remember { mutableStateOf(true) }
    var selectedCategoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }
    var showEditBudgetDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<BudgetEntry?>(null) }

    val itineraryItems = remember(plan.days) { plan.days.flatMap { it.items } }
    val staysCost = remember(plan.stays, plan.travelerCount) {
        plan.stays.sumOf { com.example.tripmate.util.CostParser.parseRupees(it.pricePerNight, plan.travelerCount) }
    }
    val diningCost = remember(plan.dining, plan.travelerCount) {
        plan.dining.sumOf { com.example.tripmate.util.CostParser.parseRupees(it.priceRange, plan.travelerCount) }
    }
    val itineraryItemsCost = remember(itineraryItems, plan.travelerCount) {
        itineraryItems.sumOf { item ->
            if (item.costAmount > 0) item.costAmount else com.example.tripmate.util.CostParser.parseRupees(item.costLabel, plan.travelerCount)
        }
    }
    val customExpensesCost = remember(plan.customExpenses) {
        plan.customExpenses.sumOf { it.amount }
    }

    val totalSpent = itineraryItemsCost + customExpensesCost +
            (if (includeStays) staysCost else 0) +
            (if (includeDining) diningCost else 0)

    val remaining = plan.budget - totalSpent
    val isOverBudget = totalSpent > plan.budget
    val progress = if (plan.budget > 0) (totalSpent.toFloat() / plan.budget).coerceIn(0f, 1f) else 0f

    val categoryTotals = remember(itineraryItems, plan.customExpenses, includeStays, includeDining, staysCost, diningCost, plan.travelerCount) {
        val map = ExpenseCategory.values().associateWith { 0 }.toMutableMap()
        itineraryItems.forEach { item ->
            val amt = if (item.costAmount > 0) item.costAmount else com.example.tripmate.util.CostParser.parseRupees(item.costLabel, plan.travelerCount)
            map[item.category] = (map[item.category] ?: 0) + amt
        }
        plan.customExpenses.forEach { exp ->
            map[exp.category] = (map[exp.category] ?: 0) + exp.amount
        }
        if (includeStays) {
            map[ExpenseCategory.STAY] = (map[ExpenseCategory.STAY] ?: 0) + staysCost
        }
        if (includeDining) {
            map[ExpenseCategory.FOOD] = (map[ExpenseCategory.FOOD] ?: 0) + diningCost
        }
        map.filterValues { it > 0 }
    }

    val dayTotals = remember(plan.days, plan.customExpenses, plan.travelerCount) {
        plan.days.map { day ->
            val dayItemsCost = day.items.sumOf { item ->
                if (item.costAmount > 0) item.costAmount else com.example.tripmate.util.CostParser.parseRupees(item.costLabel, plan.travelerCount)
            }
            val dayCustomCost = plan.customExpenses.filter { it.dayNumber == day.dayNumber }.sumOf { it.amount }
            day.dayNumber to (dayItemsCost + dayCustomCost)
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.lg)) {
        // Total Budget Card with pencil edit button
        Card(
            shape = RoundedCornerShape(Dimens.radiusCard),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(Dimens.lg)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Total Spent",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "₹$totalSpent",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            if (isOverBudget) "Over Budget" else "Remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "₹${kotlin.math.abs(remaining)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.md)
                        .height(8.dp)
                        .clip(RoundedCornerShape(Dimens.radiusFull))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(Dimens.radiusFull))
                            .background(if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "of ₹${plan.budget} budget",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { showEditBudgetDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Budget",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Over-budget banner with Replan cheaper action
        if (isOverBudget) {
            Card(
                shape = RoundedCornerShape(Dimens.radiusMd),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(Dimens.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = Dimens.sm)) {
                        Text(
                            text = "Over budget by ₹${totalSpent - plan.budget}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "TripPilot can suggest free or budget alternatives.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    FilledTonalButton(
                        onClick = onReplanCheaper,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Replan")
                    }
                }
            }
        }

        // Donut / Pie Chart by category
        Card(
            shape = RoundedCornerShape(Dimens.radiusCard),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(Dimens.md)) {
                Text(
                    text = "Spending by Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(130.dp)) {
                            val strokeWidth = 22.dp.toPx()
                            if (totalSpent == 0 || categoryTotals.isEmpty()) {
                                drawArc(
                                    color = Color.LightGray.copy(alpha = 0.4f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                            } else {
                                var currentAngle = -90f
                                categoryTotals.forEach { (cat, amount) ->
                                    val sweep = (amount.toFloat() / totalSpent.toFloat()) * 360f
                                    drawArc(
                                        color = categoryColor(cat),
                                        startAngle = currentAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                    )
                                    currentAngle += sweep
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹$totalSpent", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        categoryTotals.forEach { (cat, amount) ->
                            val pct = if (totalSpent > 0) (amount * 100 / totalSpent) else 0
                            val isSelected = selectedCategoryFilter == cat
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(Dimens.radiusSm))
                                    .background(if (isSelected) categoryColor(cat).copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        selectedCategoryFilter = if (isSelected) null else cat
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(categoryColor(cat)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${cat.name.lowercase().replaceFirstChar { it.uppercase() }}: ₹$amount ($pct%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Daily Spending Bar Chart
        if (dayTotals.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(Dimens.radiusCard),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(Dimens.md)) {
                    Text(
                        text = "Daily Spending",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val maxDaySpend = (dayTotals.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .padding(top = Dimens.md),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        dayTotals.forEach { (dayNum, spend) ->
                            val barFraction = (spend.toFloat() / maxDaySpend.toFloat()).coerceIn(0.08f, 1f)
                            val barColor = com.example.tripmate.ui.components.dayColorFor(dayNum)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "₹$spend",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .fillMaxHeight(barFraction * 0.70f)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(barColor)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "D$dayNum",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Inclusions Toggles (Stays & Dining)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Include in Budget:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                FilterChip(
                    selected = includeStays,
                    onClick = { includeStays = !includeStays },
                    label = { Text("Stays (₹$staysCost)") }
                )
                FilterChip(
                    selected = includeDining,
                    onClick = { includeDining = !includeDining },
                    label = { Text("Dining (₹$diningCost)") }
                )
            }
        }

        // Action Header: Filter indication + Add Expense button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedCategoryFilter != null) "Category: ${selectedCategoryFilter!!.name}" else "Itemized Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(
                onClick = { showAddExpenseDialog = true },
                contentPadding = PaddingValues(horizontal = Dimens.md, vertical = Dimens.xs)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Expense")
            }
        }

        // Filtered / Grouped Itemized List
        Card(
            shape = RoundedCornerShape(Dimens.radiusMd),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column {
                val filteredItems = itineraryItems.filter {
                    selectedCategoryFilter == null || it.category == selectedCategoryFilter
                }
                val filteredCustom = plan.customExpenses.filter {
                    selectedCategoryFilter == null || it.category == selectedCategoryFilter
                }

                if (filteredItems.isEmpty() && filteredCustom.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(Dimens.lg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No expenses in this category.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                filteredItems.forEachIndexed { index, item ->
                    val amt = if (item.costAmount > 0) item.costAmount else com.example.tripmate.util.CostParser.parseRupees(item.costLabel, plan.travelerCount)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Dimens.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = Dimens.md)
                        ) {
                            Icon(item.icon, contentDescription = null, tint = categoryColor(item.category), modifier = Modifier.size(18.dp))
                            Column(modifier = Modifier.padding(start = Dimens.sm)) {
                                Text(item.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    item.category.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "₹$amt",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (index != filteredItems.lastIndex || filteredCustom.isNotEmpty()) {
                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }

                filteredCustom.forEachIndexed { index, custom ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Dimens.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = Dimens.md)
                        ) {
                            Icon(Icons.Filled.Payments, contentDescription = null, tint = categoryColor(custom.category), modifier = Modifier.size(18.dp))
                            Column(modifier = Modifier.padding(start = Dimens.sm)) {
                                Text(custom.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    "${custom.category.name.lowercase().replaceFirstChar { it.uppercase() }}${custom.dayNumber?.let { " • Day $it" } ?: ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "₹${custom.amount}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { editingExpense = custom }, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onDeleteExpense(custom.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    if (index != filteredCustom.lastIndex) {
                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }

    if (showEditBudgetDialog) {
        EditBudgetDialog(
            currentBudget = plan.budget,
            onDismiss = { showEditBudgetDialog = false },
            onSave = onUpdateBudget
        )
    }

    if (showAddExpenseDialog) {
        AddOrEditExpenseDialog(
            initialEntry = null,
            dayCount = plan.days.size,
            onDismiss = { showAddExpenseDialog = false },
            onSave = onAddExpense
        )
    }

    editingExpense?.let { entry ->
        AddOrEditExpenseDialog(
            initialEntry = entry,
            dayCount = plan.days.size,
            onDismiss = { editingExpense = null },
            onSave = { updated ->
                onUpdateExpense(updated)
                editingExpense = null
            }
        )
    }
}

@Composable
private fun EditBudgetDialog(
    currentBudget: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var budgetValue by remember { mutableStateOf(currentBudget) }
    var textValue by remember { mutableStateOf(currentBudget.toString()) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Trip Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                androidx.compose.material3.OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        textValue = digits
                        digits.toIntOrNull()?.let { budgetValue = it }
                    },
                    label = { Text("Budget (₹)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Quick Adjust: ₹$budgetValue",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = budgetValue.toFloat().coerceIn(5_000f, 200_000f),
                    onValueChange = {
                        val rounded = (it / 1000).toInt() * 1000
                        budgetValue = rounded
                        textValue = rounded.toString()
                    },
                    valueRange = 5_000f..200_000f,
                    steps = 38
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(budgetValue); onDismiss() }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddOrEditExpenseDialog(
    initialEntry: BudgetEntry? = null,
    dayCount: Int,
    onDismiss: () -> Unit,
    onSave: (BudgetEntry) -> Unit
) {
    var title by remember { mutableStateOf(initialEntry?.title ?: "") }
    var amountText by remember { mutableStateOf(initialEntry?.amount?.toString() ?: "") }
    var selectedCat by remember { mutableStateOf(initialEntry?.category ?: ExpenseCategory.ACTIVITIES) }
    var selectedDay by remember { mutableStateOf(initialEntry?.dayNumber) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialEntry == null) "Add Custom Expense" else "Edit Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                androidx.compose.material3.OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense Title (e.g. Souvenirs, Taxi)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Category:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ExpenseCategory.values().forEach { cat ->
                        FilterChip(
                            selected = selectedCat == cat,
                            onClick = { selectedCat = cat },
                            label = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Text("Day (optional):", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedDay == null,
                        onClick = { selectedDay = null },
                        label = { Text("All Days") }
                    )
                    (1..dayCount).forEach { dayNum ->
                        FilterChip(
                            selected = selectedDay == dayNum,
                            onClick = { selectedDay = dayNum },
                            label = { Text("Day $dayNum") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toIntOrNull() ?: 0
                    if (title.isNotBlank() && amount > 0) {
                        onSave(
                            BudgetEntry(
                                id = initialEntry?.id ?: java.util.UUID.randomUUID().toString(),
                                title = title.trim(),
                                amount = amount,
                                category = selectedCat,
                                dayNumber = selectedDay
                            )
                        )
                        onDismiss()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditItineraryItemDialog(
    item: ItineraryItem,
    onDismiss: () -> Unit,
    onSave: (ItineraryItem) -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var time by remember { mutableStateOf(item.time) }
    var duration by remember { mutableStateOf(item.durationLabel) }
    var cost by remember { mutableStateOf(item.costLabel) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Activity") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                androidx.compose.material3.OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                androidx.compose.material3.OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    androidx.compose.material3.OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration") }, singleLine = true, modifier = Modifier.weight(1f))
                    androidx.compose.material3.OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Cost") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                val renamed = title.trim() != item.title
                // A renamed activity is a different place: drop the old location so it is re-geocoded.
                onSave(
                    item.copy(
                        title = title,
                        time = time,
                        durationLabel = duration,
                        costLabel = cost,
                        placeName = if (renamed) null else item.placeName,
                        imageUrl = if (renamed) null else item.imageUrl,
                        placeDetails = if (renamed) {
                            item.placeDetails?.copy(latitude = null, longitude = null)
                        } else item.placeDetails
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun StaysSectionView(
    stays: List<StayOption>,
    modifier: Modifier = Modifier
) {
    if (stays.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(Dimens.radiusLg)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(Dimens.xl),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No accommodations listed yet for this itinerary.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.md)
    ) {
        stays.forEach { stay ->
            Card(
                shape = RoundedCornerShape(Dimens.radiusLg),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(Dimens.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = Dimens.sm)) {
                            Surface(
                                shape = RoundedCornerShape(Dimens.radiusFull),
                                color = when (stay.tier.lowercase()) {
                                    "budget" -> MaterialTheme.colorScheme.secondaryContainer
                                    "luxury" -> MaterialTheme.colorScheme.tertiaryContainer
                                    else -> MaterialTheme.colorScheme.primaryContainer
                                }
                            ) {
                                Text(
                                    text = stay.tier.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (stay.tier.lowercase()) {
                                        "budget" -> MaterialTheme.colorScheme.onSecondaryContainer
                                        "luxury" -> MaterialTheme.colorScheme.onTertiaryContainer
                                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = Dimens.sm, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(Dimens.xs))
                            Text(
                                text = stay.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (stay.location.isNotBlank()) {
                                Text(
                                    text = stay.location,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stay.pricePerNight,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB800),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f", stay.rating),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (stay.whyRecommended.isNotBlank()) {
                        Spacer(modifier = Modifier.height(Dimens.sm))
                        Text(
                            text = stay.whyRecommended,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiningSectionView(
    dining: List<DiningOption>,
    modifier: Modifier = Modifier
) {
    if (dining.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(Dimens.radiusLg)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(Dimens.xl),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No culinary recommendations listed yet for this trip.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.md)
    ) {
        dining.forEach { place ->
            Card(
                shape = RoundedCornerShape(Dimens.radiusLg),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(Dimens.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = Dimens.sm)) {
                            Text(
                                text = place.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(Dimens.radiusFull),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = place.cuisine,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = Dimens.sm, vertical = 2.dp)
                                    )
                                }
                                if (place.priceRange.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(Dimens.radiusFull),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = place.priceRange,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = Dimens.sm, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB800),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", place.rating),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (place.famousFor.isNotBlank()) {
                        Spacer(modifier = Modifier.height(Dimens.sm))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Must try: ${place.famousFor}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}


