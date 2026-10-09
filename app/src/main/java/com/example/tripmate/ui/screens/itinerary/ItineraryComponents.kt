package com.example.tripmate.ui.screens.itinerary

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsCar
import com.example.tripmate.model.TravelLeg
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    ItineraryTab.DOCUMENTS -> Icons.Filled.ConfirmationNumber
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
    destination: String = "",
    nextItem: ItineraryItem? = null,
    onVoteClick: ((Boolean) -> Unit)? = null,
    attachedDocuments: List<com.example.tripmate.model.TripDocument> = emptyList(),
    onViewDocumentClick: ((com.example.tripmate.model.TripDocument) -> Unit)? = null,
    onAttachDocumentClick: (() -> Unit)? = null
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Timeline node + connector line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight()
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

        // Right side: Activity card + Travel leg indicator
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Dimens.md)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
                shape = RoundedCornerShape(Dimens.radiusLg),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background destination/activity photo with gradient scrim
                bgImageUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
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

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Interactive Group Voting Pill
                            Surface(
                                shape = RoundedCornerShape(Dimens.radiusFull),
                                color = Color.Black.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    val isUpvoted = item.votes.userVote == "UP"
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(Dimens.radiusFull))
                                            .background(if (isUpvoted) Color(0xFF10B981).copy(alpha = 0.35f) else Color.Transparent)
                                            .clickable(enabled = onVoteClick != null) { onVoteClick?.invoke(true) }
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ThumbUp,
                                            contentDescription = "Upvote",
                                            tint = if (isUpvoted) Color(0xFF34D399) else Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = "${item.votes.upvotes}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isUpvoted) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isUpvoted) Color(0xFF34D399) else Color.White
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .height(12.dp)
                                            .width(1.dp)
                                            .background(Color.White.copy(alpha = 0.25f))
                                    )

                                    val isDownvoted = item.votes.userVote == "DOWN"
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(Dimens.radiusFull))
                                            .background(if (isDownvoted) Color(0xFFEF4444).copy(alpha = 0.35f) else Color.Transparent)
                                            .clickable(enabled = onVoteClick != null) { onVoteClick?.invoke(false) }
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ThumbDown,
                                            contentDescription = "Downvote",
                                            tint = if (isDownvoted) Color(0xFFF87171) else Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = "${item.votes.downvotes}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isDownvoted) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isDownvoted) Color(0xFFF87171) else Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(Dimens.xs))

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
                                            text = { Text("Attach Reservation / Pass") },
                                            leadingIcon = { Icon(Icons.Filled.ConfirmationNumber, contentDescription = null) },
                                            onClick = { menuExpanded = false; onAttachDocumentClick?.invoke() }
                                        )
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
                                            text = { Text("Attach Reservation / Pass") },
                                            leadingIcon = { Icon(Icons.Filled.ConfirmationNumber, contentDescription = null) },
                                            onClick = { menuExpanded = false; onAttachDocumentClick?.invoke() }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("This is a fixed commitment", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                            onClick = { menuExpanded = false },
                                            enabled = false
                                        )
                                    }
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
                        horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InfoChip(icon = Icons.Filled.Schedule, label = item.durationLabel, isDark = true)
                        InfoChip(icon = Icons.Filled.Payments, label = item.costLabel, isDark = true)
                        if (item.votes.netScore >= 1) {
                            Surface(
                                shape = RoundedCornerShape(Dimens.radiusSm),
                                color = Color(0xFF10B981).copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "🔥 Top Pick (+${item.votes.netScore})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF34D399),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = Dimens.sm, vertical = 4.dp)
                                )
                            }
                        } else if (item.votes.netScore <= -1) {
                            Surface(
                                shape = RoundedCornerShape(Dimens.radiusSm),
                                color = Color(0xFFEF4444).copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "⚠️ Mixed (${item.votes.netScore})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF87171),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = Dimens.sm, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (attachedDocuments.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = Dimens.xs),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            attachedDocuments.forEach { doc ->
                                Surface(
                                    shape = RoundedCornerShape(Dimens.radiusSm),
                                    color = Color.Black.copy(alpha = 0.55f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = onViewDocumentClick != null) {
                                            onViewDocumentClick?.invoke(doc)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ConfirmationNumber,
                                            contentDescription = null,
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = buildString {
                                                if (!doc.confirmationNumber.isNullOrBlank()) {
                                                    append("CONF# ${doc.confirmationNumber} • ")
                                                }
                                                append(doc.title)
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (doc.fileUri != null) {
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                imageVector = if (doc.fileType == com.example.tripmate.model.DocumentFileType.PDF) Icons.Filled.PictureAsPdf else Icons.Filled.Image,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
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

        if (!isLastItem) {
            val leg = item.travelToNext ?: computeTravelLeg(item, nextItem)
            if (leg != null) {
                Row(
                    modifier = Modifier.padding(start = Dimens.xs, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(Dimens.radiusFull),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Dimens.sm, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DirectionsWalk,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${leg.distanceLabel} • ${leg.durationLabel} (${leg.transportMode})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(Dimens.sm))
            }
        } else {
            Spacer(modifier = Modifier.height(Dimens.xs))
        }
    }
}
}

private fun computeTravelLeg(item: ItineraryItem, nextItem: ItineraryItem?): TravelLeg? {
    if (nextItem == null) return null
    val lat1 = item.placeDetails?.latitude ?: return null
    val lng1 = item.placeDetails?.longitude ?: return null
    val lat2 = nextItem.placeDetails?.latitude ?: return null
    val lng2 = nextItem.placeDetails?.longitude ?: return null

    val distKm = distanceBetweenKm(lat1, lng1, lat2, lng2)
    val distLabel = if (distKm < 0.95) {
        "${((distKm * 10).toInt() * 100).coerceAtLeast(100)} m"
    } else {
        String.format(java.util.Locale.US, "%.1f km", distKm)
    }

    val mode = if (distKm <= 1.0) "Walk" else "Drive"
    val avgSpeed = if (mode == "Walk") 4.5 else 25.0
    val mins = kotlin.math.max(2, (distKm / avgSpeed * 60).toInt())
    val durationLabel = if (mins >= 60) "${mins / 60} hr ${mins % 60} mins" else "$mins mins"

    return TravelLeg(
        distanceLabel = distLabel,
        durationLabel = durationLabel,
        transportMode = mode
    )
}

private fun distanceBetweenKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lon2 - lon1)
    val a = (kotlin.math.sin(dLat / 2).let { it * it } +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLng / 2).let { it * it }).coerceIn(0.0, 1.0)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt((1.0 - a).coerceIn(0.0, 1.0)))
    return 6371.0 * c
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
    modifier: Modifier = Modifier,
    onOpenExpenses: ((String) -> Unit)? = null
) {
    var selectedCategoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }
    var showEditBudgetDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<BudgetEntry?>(null) }
    var showAiEstimates by remember { mutableStateOf(false) }

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
    // Only user-added expenses count as spent (AI recommendations never auto-count as spent)
    val totalSpent = remember(plan.customExpenses) {
        plan.customExpenses.sumOf { it.amount }
    }

    val remaining = plan.budget - totalSpent
    val isOverBudget = totalSpent > plan.budget
    val progress = if (plan.budget > 0) (totalSpent.toFloat() / plan.budget).coerceIn(0f, 1f) else 0f

    val categoryTotals = remember(plan.customExpenses) {
        val map = ExpenseCategory.values().associateWith { 0 }.toMutableMap()
        plan.customExpenses.forEach { exp ->
            map[exp.category] = (map[exp.category] ?: 0) + exp.amount
        }
        map.filterValues { it > 0 }
    }

    val dayTotals = remember(plan.days, plan.customExpenses) {
        plan.days.mapNotNull { day ->
            val dayCustomCost = plan.customExpenses.filter { it.dayNumber == day.dayNumber }.sumOf { it.amount }
            if (dayCustomCost > 0) day.dayNumber to dayCustomCost else null
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

        // Group Split & Shared Members Card
        val effectiveGroupMembers = maxOf(plan.travelerCount, plan.membersCount, 1)
        if (effectiveGroupMembers > 1 || plan.isShared) {
            val perPersonBudget = plan.budget / effectiveGroupMembers
            val perPersonSpent = totalSpent / effectiveGroupMembers
            Card(
                shape = RoundedCornerShape(Dimens.radiusCard),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(Dimens.lg)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(Dimens.xs))
                            Text(
                                text = "Group Split ($effectiveGroupMembers Travelers)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (plan.isShared) {
                            Surface(
                                shape = RoundedCornerShape(Dimens.radiusFull),
                                color = Color(0xFF10B981).copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "Live Shared",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = Dimens.sm),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Fair Share / Person", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹$perPersonSpent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Budget / Person", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹$perPersonBudget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (plan.supabaseTripId != null && onOpenExpenses != null) {
                        Spacer(Modifier.height(Dimens.md))
                        FilledTonalButton(
                            onClick = { onOpenExpenses(plan.supabaseTripId) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Dimens.radiusFull)
                        ) {
                            Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Manage Group Expenses & Balances", style = MaterialTheme.typography.labelMedium)
                        }
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
                            text = "TripMate can suggest free or budget alternatives.",
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(end = Dimens.md)
                    ) {
                        Canvas(modifier = Modifier.size(120.dp)) {
                            val strokeWidth = 20.dp.toPx()
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

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categoryTotals.forEach { (cat, amount) ->
                            val pct = if (totalSpent > 0) (amount * 100 / totalSpent) else 0
                            val isSelected = selectedCategoryFilter == cat
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                    val barScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(barScrollState)
                            .padding(top = Dimens.md, bottom = Dimens.xs),
                        horizontalArrangement = if (dayTotals.size <= 5) Arrangement.SpaceEvenly else Arrangement.spacedBy(Dimens.lg),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        dayTotals.forEach { (dayNum, spend) ->
                            val barFraction = (spend.toFloat() / maxDaySpend.toFloat()).coerceIn(0.08f, 1f)
                            val barColor = com.example.tripmate.ui.components.dayColorFor(dayNum)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = if (dayTotals.size <= 5) Modifier.weight(1f) else Modifier.widthIn(min = 48.dp)
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
                                        .height(64.dp)
                                        .width(22.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height((64 * barFraction).dp)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(barColor)
                                    )
                                }
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

        // Action Header: Filter indication + Add Expense button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (selectedCategoryFilter != null) "Category: ${selectedCategoryFilter!!.name}" else "User-Logged Expenses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (plan.isShared || plan.membersCount > 1) "${plan.customExpenses.size} shared expense(s)" else "${plan.customExpenses.size} expense(s) logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = { showAddExpenseDialog = true },
                contentPadding = PaddingValues(horizontal = Dimens.md, vertical = Dimens.xs)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Expense")
            }
        }

        // Filtered / Grouped Itemized List (Only user-added expenses — no AI auto-added expenses)
        Card(
            shape = RoundedCornerShape(Dimens.radiusMd),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column {
                val filteredCustom = plan.customExpenses.filter {
                    selectedCategoryFilter == null || it.category == selectedCategoryFilter
                }

                if (filteredCustom.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(Dimens.lg),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Payments,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(Dimens.xs))
                            Text(
                                text = if (selectedCategoryFilter != null) "No expenses in this category." else "No expenses logged yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (plan.isShared || plan.membersCount > 1)
                                    "Tap '+ Add Expense' or use 'Group Expenses' to log spending."
                                else
                                    "Tap '+ Add Expense' to record spending.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                                val catName = custom.category.name.lowercase().replaceFirstChar { it.uppercase() }
                                val dayText = custom.dayNumber?.let { " • Day $it" } ?: ""
                                val splitText = if (plan.isShared || plan.membersCount > 1) " • Shared" else ""
                                Text(
                                    "$catName$dayText$splitText",
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

        // AI Estimates Reference Card (Informational only — does NOT count towards spent)
        Card(
            shape = RoundedCornerShape(Dimens.radiusCard),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(Dimens.md)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAiEstimates = !showAiEstimates },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Projected Costs (Planning Reference)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Estimated costs — not counted towards spent expenses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (showAiEstimates) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AnimatedVisibility(visible = showAiEstimates) {
                    Column(modifier = Modifier.padding(top = Dimens.md), verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Planned Activities (AI)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹$itineraryItemsCost", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Recommended Stays (AI)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹$staysCost", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Recommended Dining (AI)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹$diningCost", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total AI Projected Cost", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("₹${itineraryItemsCost + staysCost + diningCost}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
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
    var selectedCategory by remember { mutableStateOf(item.category) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Activity") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                androidx.compose.material3.OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                androidx.compose.material3.OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    androidx.compose.material3.OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration") }, singleLine = true, modifier = Modifier.weight(1f))
                    androidx.compose.material3.OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Cost") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Text("Category:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ExpenseCategory.values().forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                val renamed = title.trim() != item.title
                val parsedCost = com.example.tripmate.util.CostParser.parseRupees(cost)
                // A renamed activity is a different place: drop the old location so it is re-geocoded.
                onSave(
                    item.copy(
                        title = title.trim(),
                        time = time.trim(),
                        durationLabel = duration.trim(),
                        costLabel = cost.trim(),
                        costAmount = parsedCost,
                        category = selectedCategory,
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.xs)) {
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
                                if (stay.rating >= 4.5) {
                                    Surface(
                                        shape = RoundedCornerShape(Dimens.radiusFull),
                                        color = Color(0xFFFEF3C7)
                                    ) {
                                        Text(
                                            text = "★ Top Rated",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = Dimens.sm, vertical = 2.dp)
                                        )
                                    }
                                }
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

                    val context = androidx.compose.ui.platform.LocalContext.current
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            val query = android.net.Uri.encode("${stay.name}, ${stay.location}")
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=$query"))
                            runCatching { context.startActivity(intent) }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = Dimens.sm),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View on Map", style = MaterialTheme.typography.labelMedium)
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


