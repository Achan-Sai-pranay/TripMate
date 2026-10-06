package com.example.tripmate.ui.screens.itinerary

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tripmate.model.AssistantItineraryPin
import com.example.tripmate.model.ItineraryDay
import com.example.tripmate.model.ItineraryItem
import com.example.tripmate.ui.components.TripMap
import com.example.tripmate.ui.components.dayColorFor
import com.example.tripmate.ui.theme.Dimens

/** 1-based position of the item among its day's map places, or null for generic (non-place) activities. */
fun ItineraryDay.placeOrder(itemIndex: Int): Int? {
    if (items.getOrNull(itemIndex)?.placeName?.isBlank() == true) return null
    return items.take(itemIndex + 1).count { it.placeName?.isBlank() != true }
}

fun pinKeyFor(dayNumber: Int, order: Int, title: String) = "$dayNumber#$order#$title"

/** Key of the map pin that belongs to this itinerary item (null if it has no map place). */
fun ItineraryDay.pinKey(itemIndex: Int): String? {
    val order = placeOrder(itemIndex) ?: return null
    return pinKeyFor(dayNumber, order, items[itemIndex].title)
}

/** Itinerary places -> map pins. Places that haven't been geocoded yet have null coordinates. */
fun ItineraryDay.toPins(destination: String): List<AssistantItineraryPin> =
    items.indices.mapNotNull { index ->
        val item = items[index]
        val order = placeOrder(index) ?: return@mapNotNull null
        AssistantItineraryPin(
            title = item.title,
            dayNumber = dayNumber,
            location = "${item.geocodeQuery ?: item.title}, $destination",
            latitude = item.placeDetails?.latitude,
            longitude = item.placeDetails?.longitude,
            order = order,
            visitTime = item.time.takeIf { it.isNotBlank() && it != "TBD" }
        )
    }

/** Map card shown at the top of the Itinerary tab for the selected day. */
@Composable
fun ItineraryMapCard(
    destination: String,
    day: ItineraryDay,
    selectedKey: String?,
    focusToken: Int,
    isResolving: Boolean,
    onPinSelected: (AssistantItineraryPin) -> Unit,
    modifier: Modifier = Modifier
) {
    val pins = remember(day) { day.toPins(destination) }
    if (pins.isEmpty()) return
    Card(
        shape = RoundedCornerShape(Dimens.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        TripMap(
            pins = pins,
            selectedKey = selectedKey,
            onPinSelected = onPinSelected,
            focusToken = focusToken,
            isResolving = isResolving,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(Dimens.radiusLg))
        )
    }
}

@Composable
fun InteractiveMapTab(
    destination: String,
    days: List<ItineraryDay>,
    selectedKey: String?,
    focusToken: Int,
    isResolving: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // null = all days
    var dayFilter by remember { mutableStateOf<Int?>(null) }
    val visibleDays = remember(days, dayFilter) { days.filter { dayFilter == null || it.dayNumber == dayFilter } }
    val pins = remember(visibleDays, destination) { visibleDays.flatMap { it.toPins(destination) } }
    val located = pins.count { it.hasCoordinates }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
        Card(
            shape = RoundedCornerShape(Dimens.radiusLg),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(Dimens.md), verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (dayFilter == null) "All days" else "Route for Day $dayFilter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$located of ${pins.size} places pinned in $destination",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            val target = pins.firstOrNull { it.key == selectedKey }
                            val uri = if (target?.hasCoordinates == true) {
                                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${target.latitude},${target.longitude}")
                            } else {
                                Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(destination)}")
                            }
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        },
                        contentPadding = PaddingValues(horizontal = Dimens.md, vertical = Dimens.xs)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Navigate", style = MaterialTheme.typography.labelSmall)
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    item {
                        FilterChip(selected = dayFilter == null, onClick = { dayFilter = null }, label = { Text("All days") })
                    }
                    items(days, key = { it.dayNumber }) { d ->
                        FilterChip(
                            selected = dayFilter == d.dayNumber,
                            onClick = { dayFilter = d.dayNumber },
                            label = { Text("Day ${d.dayNumber}") }
                        )
                    }
                }
            }
        }

        if (pins.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(Dimens.radiusLg),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                TripMap(
                    pins = pins,
                    selectedKey = selectedKey,
                    onPinSelected = { onSelect(it.key) },
                    focusToken = focusToken,
                    isResolving = isResolving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(Dimens.radiusLg))
                )
            }
        }

        // Stops list: tapping one focuses it on the map; tapping a marker highlights it here.
        visibleDays.forEach { day ->
            if (dayFilter == null) {
                Text(
                    text = "Day ${day.dayNumber} • ${day.dateLabel}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = dayColorFor(day.dayNumber)
                )
            }
            day.items.forEachIndexed { index, item ->
                val order = day.placeOrder(index) ?: return@forEachIndexed
                val key = pinKeyFor(day.dayNumber, order, item.title)
                StopRow(
                    item = item,
                    order = order,
                    dayNumber = day.dayNumber,
                    isSelected = key == selectedKey,
                    onClick = { onSelect(key) },
                    showLeg = index < day.items.lastIndex
                )
            }
        }
    }
}

@Composable
private fun StopRow(
    item: ItineraryItem,
    order: Int,
    dayNumber: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    showLeg: Boolean
) {
    val dayColor = dayColorFor(dayNumber)
    Card(
        shape = RoundedCornerShape(Dimens.radiusMd),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) dayColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) dayColor else MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(Dimens.md), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(dayColor),
                contentAlignment = Alignment.Center
            ) {
                Text("$order", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.width(Dimens.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (item.hasCoordinates) item.time else "${item.time} • locating…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                item.placeDetails?.let { details ->
                    Row(
                        modifier = Modifier.padding(top = Dimens.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFB800), modifier = Modifier.size(13.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f (%d)", details.rating, details.reviewCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    if (showLeg) {
        item.travelToNext?.let { leg ->
            Row(
                modifier = Modifier.padding(start = Dimens.md, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(Dimens.radiusFull),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Dimens.sm, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.DirectionsWalk, contentDescription = null,
                            modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant
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
        }
    }
}
