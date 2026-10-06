package com.example.tripmate.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tripmate.model.AssistantItineraryPin
import com.example.tripmate.model.AssistantMapRoute
import com.example.tripmate.ui.theme.Dimens
import com.example.tripmate.ui.theme.SubtleBorder
import com.example.tripmate.ui.theme.TextPrimary
import com.example.tripmate.ui.theme.TextSecondary

private const val TAG = "AssistantMap"
private val MapBlue = Color(0xFF2563EB)

/**
 * Collapsible map panel for the assistant screen.
 *
 * - Collapsed: slim header bar so chat keeps ~85% of screen height.
 * - Expanded: fully interactive map with real pins at coordinates,
 *   touch pan/zoom, auto-framing, and horizontal stop cards.
 */
@Composable
fun InteractiveAssistantMap(
    route: AssistantMapRoute,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    focusPinTitle: String? = null,
    focusRequestId: Int = 0
) {
    val context = LocalContext.current
    var selectedKey by remember(route.destination) { mutableStateOf<String?>(null) }
    var focusToken by remember { mutableIntStateOf(0) }
    val stripState = rememberLazyListState()
    val selectedIndex = route.pins.indexOfFirst { it.key == selectedKey }
    val resolving = route.pins.any { !it.hasCoordinates }

    // Default selection: first place that has a real location
    LaunchedEffect(route) {
        if (route.pins.none { it.key == selectedKey }) {
            selectedKey = route.pins.firstOrNull { it.hasCoordinates }?.key
        }
    }

    // Sync focused pin from chat response (tapping a place name in a chat message)
    LaunchedEffect(focusRequestId, route) {
        val title = focusPinTitle ?: return@LaunchedEffect
        val pin = route.pins.firstOrNull {
            it.title.equals(title, ignoreCase = true) ||
                it.title.contains(title, ignoreCase = true) ||
                title.contains(it.title, ignoreCase = true)
        } ?: return@LaunchedEffect
        selectedKey = pin.key
        focusToken++
    }

    // Map -> list: keep the stop-cards strip scrolled to the active pin
    LaunchedEffect(selectedIndex, expanded) {
        if (expanded && selectedIndex >= 0) stripState.animateScrollToItem(selectedIndex)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MapHeader(
                route = route,
                expanded = expanded,
                onToggle = { onExpandedChange(!expanded) },
                onOpenExternal = {
                    launchExternalMaps(context, route.pins.getOrNull(selectedIndex), route.destination)
                },
                onDismiss = onDismiss
            )

            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(horizontal = Dimens.sm)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, SubtleBorder, RoundedCornerShape(14.dp))
                        .background(Color(0xFFE8EEF3))
                ) {
                    TripMap(
                        pins = route.pins,
                        selectedKey = selectedKey,
                        onPinSelected = { selectedKey = it.key },
                        focusToken = focusToken,
                        isResolving = resolving,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                LazyRow(
                    state = stripState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.sm, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(route.pins) { index, pin ->
                        StopCardItem(
                            pin = pin,
                            index = index,
                            isSelected = pin.key == selectedKey,
                            onClick = {
                                selectedKey = pin.key
                                focusToken++
                            },
                            onNavigateClick = { launchExternalMaps(context, pin, route.destination) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapHeader(
    route: AssistantMapRoute,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenExternal: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = Dimens.md, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0xFFEFF6FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MapBlue, modifier = Modifier.size(17.dp))
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = route.itineraryTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (expanded) "Tap a pin or stop to focus" else "${route.pins.size} stops • Tap to view map",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextSecondary,
                maxLines = 1
            )
        }

        if (expanded) {
            IconButton(onClick = onOpenExternal, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open in Google Maps", tint = MapBlue, modifier = Modifier.size(18.dp))
            }
        }

        IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                contentDescription = if (expanded) "Collapse map" else "Expand map",
                tint = Color(0xFF475569),
                modifier = Modifier.size(22.dp)
            )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Close map", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun StopCardItem(
    pin: AssistantItineraryPin,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onNavigateClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSelected) MapBlue else Color(0xFFE2E8F0)),
        modifier = Modifier.width(190.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(dayColorFor(pin.dayNumber)),
                contentAlignment = Alignment.Center
            ) {
                Text("${pin.order.takeIf { it > 0 } ?: (index + 1)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pin.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (pin.hasCoordinates) "Day ${pin.dayNumber}" else "Day ${pin.dayNumber} • locating…",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextSecondary,
                    maxLines = 1
                )
            }
            IconButton(onClick = onNavigateClick, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Filled.NearMe,
                    contentDescription = "Navigate in Google Maps",
                    tint = if (isSelected) MapBlue else Color(0xFF94A3B8),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

private fun launchExternalMaps(context: Context, pin: AssistantItineraryPin?, destination: String) {
    try {
        if (pin?.latitude != null && pin.longitude != null) {
            val geoUri = Uri.parse("geo:${pin.latitude},${pin.longitude}?q=${pin.latitude},${pin.longitude}(${Uri.encode(pin.title)})")
            val intent = Intent(Intent.ACTION_VIEW, geoUri).setPackage("com.google.android.apps.maps")
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                val web = Uri.parse("https://www.google.com/maps/search/?api=1&query=${pin.latitude},${pin.longitude}")
                context.startActivity(Intent(Intent.ACTION_VIEW, web))
            }
        } else {
            val query = if (pin != null) "${pin.title}, $destination" else destination
            val web = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")
            context.startActivity(Intent(Intent.ACTION_VIEW, web))
        }
    } catch (e: Exception) {
        Log.w(TAG, "No app available to open maps", e)
    }
}
