package com.example.tripmate.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tripmate.model.AssistantMapRoute
import com.example.tripmate.ui.theme.Dimens
import com.example.tripmate.ui.theme.PrimaryOrange
import com.example.tripmate.ui.theme.SubtleBorder
import com.example.tripmate.ui.theme.TextMuted
import com.example.tripmate.ui.theme.TextPrimary
import com.example.tripmate.ui.theme.TextSecondary

@Composable
fun AssistantBottomMapRoute(
    route: AssistantMapRoute,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.md, vertical = Dimens.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(Dimens.xs))
                    Column {
                        Text(
                            text = route.itineraryTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${route.pins.size} pinned stops in ${route.destination}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = {
                            val mapUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode("${route.pins.firstOrNull()?.title ?: route.destination}, ${route.destination}")}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, mapUri))
                        },
                        shape = RoundedCornerShape(Dimens.radiusFull)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open Map", style = MaterialTheme.typography.labelSmall)
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close map",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Map preview with pinned dots
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(0xFFE2E8F0))
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1524661135-423995f22d0b?auto=format&fit=crop&w=800&q=80",
                    contentDescription = "Map view",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )

                // Map tint
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))

                // Pinned markers row on map
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Dimens.md),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    route.pins.take(4).forEachIndexed { index, pin ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                val mapUri = Uri.parse("geo:0,0?q=${Uri.encode("${pin.title}, ${route.destination}")}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, mapUri))
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryOrange),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(Dimens.radiusFull),
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = pin.title.take(12),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Horizontal scrolling stops cards
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.sm, vertical = Dimens.xs),
                horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
            ) {
                itemsIndexed(route.pins) { index, pin ->
                    Card(
                        onClick = {
                            val mapUri = Uri.parse("geo:0,0?q=${Uri.encode("${pin.title}, ${route.destination}")}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, mapUri))
                        },
                        shape = RoundedCornerShape(Dimens.radiusMd),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SubtleBorder),
                        modifier = Modifier.width(180.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(Dimens.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryOrange.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryOrange
                                )
                            }
                            Spacer(modifier = Modifier.width(Dimens.xs))
                            Column {
                                Text(
                                    text = pin.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Day ${pin.dayNumber}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
