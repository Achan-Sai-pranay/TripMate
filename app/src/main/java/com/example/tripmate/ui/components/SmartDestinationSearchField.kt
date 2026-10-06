package com.example.tripmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tripmate.data.DestinationRanker
import com.example.tripmate.data.DestinationSearchRepository
import com.example.tripmate.data.DestinationSuggestion
import com.example.tripmate.ui.theme.Dimens
import com.example.tripmate.ui.theme.PrimaryOrange
import com.example.tripmate.ui.theme.SubtleBorder
import com.example.tripmate.ui.theme.TextMuted
import com.example.tripmate.ui.theme.TextPrimary
import com.example.tripmate.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SmartDestinationSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onDestinationSelected: (DestinationSuggestion) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search destinations (e.g. Goa, Paris)"
) {
    var suggestions by remember { mutableStateOf<List<DestinationSuggestion>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var suppressNextSearch by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (suppressNextSearch) {
            suppressNextSearch = false
            suggestions = emptyList()
            isSearching = false
            return@LaunchedEffect
        }

        val q = query.trim()
        if (q.isEmpty()) {
            suggestions = emptyList()
            isSearching = false
            return@LaunchedEffect
        }

        // Stage 1: show offline curated matches instantly with 0 delay
        val offlineMatches = DestinationSearchRepository.searchOffline(q)
        suggestions = offlineMatches

        // Stage 2: fetch online Photon results after 200ms debounce and merge without evicting offline
        isSearching = true
        delay(200)
        try {
            val onlineMatches = DestinationSearchRepository.searchOnline(q)
            if (onlineMatches.isNotEmpty()) {
                val combined = mutableListOf<Pair<DestinationSuggestion, Boolean>>()
                offlineMatches.forEach { combined.add(it to true) }
                onlineMatches.forEach { combined.add(it to false) }
                suggestions = DestinationRanker.rankAndDedupe(combined, q).take(8)
            }
        } catch (_: Exception) {
            // Keep instant offline matches intact
        } finally {
            isSearching = false
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Modern Search Bar matching Screenshot 4
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(Dimens.radiusFull))
                .background(Color(0xFFF1F5F9))
                .padding(horizontal = Dimens.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = TextSecondary,
                modifier = Modifier
                    .padding(start = Dimens.xs, end = Dimens.xs)
                    .size(22.dp)
            )

            OutlinedTextField(
                value = query,
                onValueChange = {
                    suppressNextSearch = false
                    onQueryChange(it)
                },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = PrimaryOrange
                )
            )

            if (isSearching) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).padding(end = Dimens.xs),
                    strokeWidth = 2.dp,
                    color = PrimaryOrange
                )
            } else if (query.isNotEmpty()) {
                IconButton(
                    onClick = {
                        suppressNextSearch = false
                        suggestions = emptyList()
                        onQueryChange("")
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Suggestions Dropdown List with Pin icons and @handles
        if (query.isNotEmpty() && suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Dimens.xs))
            Card(
                shape = RoundedCornerShape(Dimens.radiusLg),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, SubtleBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.xs)
            ) {
                Column(modifier = Modifier.padding(vertical = Dimens.xs)) {
                    suggestions.take(6).forEachIndexed { index, dest ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    suppressNextSearch = true
                                    suggestions = emptyList()
                                    onDestinationSelected(dest)
                                }
                                .padding(horizontal = Dimens.md, vertical = Dimens.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Location pin circle
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = PrimaryOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(Dimens.md))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dest.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${dest.region}, ${dest.country}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            dest.handle?.let { handle ->
                                Surface(
                                    shape = RoundedCornerShape(Dimens.radiusFull),
                                    color = Color(0xFFF8FAFC)
                                ) {
                                    Text(
                                        text = handle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        modifier = Modifier.padding(horizontal = Dimens.xs, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (index < suggestions.take(6).lastIndex) {
                            HorizontalDivider(
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.padding(horizontal = Dimens.md)
                            )
                        }
                    }
                }
            }
        }
    }
}
