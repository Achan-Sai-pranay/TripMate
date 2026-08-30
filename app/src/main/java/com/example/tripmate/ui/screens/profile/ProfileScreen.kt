package com.example.tripmate.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HelpCenter
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tripmate.model.SavedTrip
import com.example.tripmate.model.TravelGroup
import com.example.tripmate.model.TravelStat
import com.example.tripmate.ui.components.BottomNavTab
import com.example.tripmate.ui.components.TripPilotBottomNav
import com.example.tripmate.ui.theme.Dimens

private const val PROFILE_TOPBAR_AVATAR_URL =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuA5JivwrfVp4dfw3cO6twCGzJL71C4dBSnQB7nLUb9cVq1UgFCAQMiM-UAyxsYwYb3B3uBVcigO09jdB9bsz1T1JVGH9jNhFd0UiEAtxb7hMbj7z8xKynRolAdVIykhSXYm6r1nyQGvne76jVEa9knXdU0d0gqY2XdW11wDvwjJD9lfIL25PrJCdnxEPS5U404dCkI0X3cSf0VoB7_kawzNt5Yit-voyJ2C6ucnG06MDBYfNW0Pmkvm3A"

private const val PROFILE_HERO_AVATAR_URL =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuACn99coQ7J1NMVAVdMmsm-gG_8p4Qu9SYp0hB_CBLQWQ2Pag_RCpNZBaY5Q_NM-L59WGrm3Z8WkDhp79zRXgefpRWB9hKS4VAdF2V16HCe6WIg8x3S_nwlxzQN-P_dUkN4tkLmUFUZW5N1GUS2XGJkDa85XmZr--MXAVmpL_u7-1SN88UmZ97tLS5nG4_ho-MdHoIceOL_X0FZQRkkfzBf5Ru_OWoNTTjmXY8t-SOG2qz5QiwtzyOOFA"

@Composable
fun ProfileScreen(
    onExploreClick: () -> Unit,
    onMyTripsClick: () -> Unit,
    onAssistantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats = remember {
        listOf(
            TravelStat("12", "Trips Taken"),
            TravelStat("8", "Countries Visited"),
            TravelStat("2.5k", "Budget Saved", isTertiary = true)
        )
    }
    val savedTrips = remember {
        listOf(
            SavedTrip(
                title = "Amalfi Coast",
                subtitle = "Italy • 7 Days",
                imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDFqldVIx2-fsD7x6Beb4p2-WsGYlR-hvC7Fai2lL8ZmfINle9wOHsVzHavvTSNtBpRAi42DsFIURT5NJeWvHmEHbGkCdseydgkkyd87n5B-ouH_yg7z4iOSJKryhsI4zgi7RIxB7bHxL478jxHRda8ZzfO7rzZNQ2O7LzT_PcBg2VdsDhfKGVQpvuw8icy4yQiMS2CT45wprPPnCE88hIJLHXQQJxt8h1FpW4ssqDYCmq_izk9DSP6-w"
            ),
            SavedTrip(
                title = "Kyoto Autumn",
                subtitle = "Japan • 10 Days",
                imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBnHY2RD5xgJ8O-1Tlqry8IkPjUOTUVhLtynsJdiGfjBhujt5HklfDn67-_JvoM4dhbZQord-s7CfJJFmr0-jp99_QF7V8JHFqk6Xd4Ez6qj8di6yzweJSaorikyFZUpV8EuJnI8eigR3TyNM4VADnEnsbl0atifZPkuyOg5I7U9PSiA0vjKbVTFAvyoq9ch_LyeHUpd7DdrqMvRKMvzPncLqxlf7sIRjcuUJCLT6hETKFPXplu_OXk6Q"
            ),
            SavedTrip(
                title = "Swiss Alps Retreat",
                subtitle = "Switzerland • 5 Days",
                imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBtWGYGEmP8a5v7I-tesZZ88py2xAcnZpwTJ_uHtaiy_ARshuzgxpdgT92wcx7dVokvv1mw76bSxeqW9Gw7Or91rmKkNYZk6P15a7qIjFcPnDMEVJVp1ftfToXDhqe4hHa8qGdUp5ut1zgkwg276A4pFWZ0eXzKEpbgbsRRijl4kDJNk6lkUqp0fLXZIiGASWUU8NlMvMC3S2jmUDd4cXhKPjfAWuvzmGy5CDESm1CdhSYXnnKL-_FW1w"
            )
        )
    }
    val travelGroups = remember {
        listOf(
            TravelGroup(
                name = "Euro Trip '24",
                memberCountLabel = "4 members",
                memberAvatarUrls = listOf(
                    "https://lh3.googleusercontent.com/aida-public/AB6AXuCwpkeb-zRyue58ItTCPnloYQHCBbm8Nu6rKC03QfP7sMZri6_FJIJurD0dRUdfwX5MIgahnvyfzRwegK-7oLJ_HU3x1HM4OVx-RSunGv51uKEj2OlOxWjC-Wb2PXWKh8HCJzgp_Jn5Hy750t9TXgSDnjoON6MQ0a-G5MB9uK86ZJsB-yZwh3xcpB5TH7cqSw3QhQVtc8nSiThSPNuiAq7bOinTzcCoaIGiDLlekI_gPsUDstHicJp6Jw",
                    "https://lh3.googleusercontent.com/aida-public/AB6AXuAg13ZkeNwbNW058xp7TQVfa87kMxgABZgvOT9w6JpZWLlhgNhHsatvE7BohMFAiwMtdVX8kHecQP0nJtv21zkVi6hHTiBjI2sBTBggm6IDXKE-1URhrUaTg4FFxM6lxlI34JNhDsyppiF5_YagXLcqWu9g8SSXiaMh8rkXkvjq709CGqf3u-mxTX5bdWLOnXjTG2_qeEZHOs1vk4pxqofVstAN_ygqXL09wmswTVDNVWN4XeixTu6xKg"
                ),
                overflowCount = 2
            ),
            TravelGroup(
                name = "Weekend Hikers",
                memberCountLabel = "2 members",
                memberAvatarUrls = listOf(
                    "https://lh3.googleusercontent.com/aida-public/AB6AXuBbZ8n41kNui9jA_MP6jSePnuq7Lw29E1HkLN7pTYF4NmFNE1jJDN1dkIMkLYKE9_EbQ5S3zR06Ly_RF88A71oORWUmU7_qbFgnkFG0uXz_QurVW-h_JoCRVP4rhiObC96NDNi3bjMQlryPhpIKRkiQYC5-H0zCMoACzuidSbCfeW09lI00NxMCmhefTGQfi8TlOoiJbaRDU5Zjp-MPXWIWVtiqjDQtSiq6ieUNlLDx0ff-CYM5EZ_nUA",
                    "https://lh3.googleusercontent.com/aida-public/AB6AXuAVvyQidVd0ZdTQfu2RB0ZYp9ndLoBEFs_hI-HvRzo34xvSIKLGoVRLSt-ARJ4Q2MGg54TevZ_eYZM2oFew8o3zspsB1FmOhOed8MfgTFk6uSYuvO0zcOV7IaUqznjp4SlYWmOxEthYrXfm_e3Tr8D7dgHiHwrqbgYd7YPCTjEWFcCGs_6aWLF3VVJsJ7I2RQ7lkuxSvW5B5gWx4ZvbjkDXAhsCdOdOdISCSnMenbywti9rbg4_ajEWlw"
                )
            )
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ProfileTopBar(
                avatarUrl = PROFILE_TOPBAR_AVATAR_URL,
                onNotificationClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("You have no new notifications.")
                    }
                }
            )
        },
        bottomBar = {
            TripPilotBottomNav(
                selectedTab = BottomNavTab.PROFILE,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomNavTab.EXPLORE -> onExploreClick()
                        BottomNavTab.MY_TRIPS -> onMyTripsClick()
                        BottomNavTab.ASSISTANT -> onAssistantClick()
                        BottomNavTab.PROFILE -> { /* already here */ }
                    }
                }
            )
        }
    ) { innerPadding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimens.marginMobile,
                end = Dimens.marginMobile,
                top = Dimens.lg,
                bottom = innerPadding.calculateBottomPadding() + Dimens.lg
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.lg)
        ) {
            item { ProfileHero(avatarUrl = PROFILE_HERO_AVATAR_URL) }
            item { TravelStatsRow(stats) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Saved Trips",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.md)) {
                        items(savedTrips) { trip -> SavedTripCard(trip) }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                    Text(
                        text = "My Travel Groups",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Card(
                        shape = RoundedCornerShape(Dimens.radiusMd),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Column {
                            travelGroups.forEachIndexed { index, group ->
                                TravelGroupRow(group)
                                if (index != travelGroups.lastIndex) {
                                    androidx.compose.material3.HorizontalDivider(
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Dimens.sm),
                    modifier = Modifier.padding(top = Dimens.md)
                ) {
                    SettingsRow(
                        icon = Icons.Filled.Settings,
                        label = "Account Settings",
                        onClick = { /* wired up in the features pass */ }
                    )
                    SettingsRow(
                        icon = Icons.Filled.HelpCenter,
                        label = "Help & Support",
                        onClick = { /* wired up in the features pass */ }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimens.md),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Logout",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = Dimens.sm)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTopBar(
    avatarUrl: String,
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.marginMobile, vertical = Dimens.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            Text(
                text = "TripPilot",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = Dimens.xs)
            )
        }
        IconButton(onClick = onNotificationClick) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileHero(avatarUrl: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = "Profile photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
        )
        Text(
            text = "Arjun Sharma",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = Dimens.md)
        )
        Row(
            modifier = Modifier
                .padding(top = Dimens.xs)
                .clip(RoundedCornerShape(Dimens.radiusFull))
                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f),
                    RoundedCornerShape(Dimens.radiusFull)
                )
                .padding(horizontal = Dimens.sm, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Explorer Level",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun TravelStatsRow(stats: List<TravelStat>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
    ) {
        stats.forEach { stat ->
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(Dimens.radiusMd),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stat.value,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (stat.isTertiary) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stat.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedTripCard(trip: SavedTrip, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .width(180.dp)
            .aspectRatio(4f / 5f),
        shape = RoundedCornerShape(Dimens.radiusMd),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = trip.imageUrl,
                contentDescription = trip.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 150f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Dimens.md)
            ) {
                Text(text = trip.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(
                    text = trip.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun TravelGroupRow(group: TravelGroup, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                group.memberAvatarUrls.forEachIndexed { index, url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .offset(x = (index * 28).dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape)
                    )
                }
                group.overflowCount?.let { count ->
                    Box(
                        modifier = Modifier
                            .offset(x = (group.memberAvatarUrls.size * 28).dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(2.dp, MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$count",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(
                    start = (group.memberAvatarUrls.size * 28 + (group.overflowCount?.let { 28 } ?: 0) + 12).dp
                )
            ) {
                Text(text = group.name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp), color = MaterialTheme.colorScheme.onBackground)
                Text(
                    text = group.memberCountLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusMd),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = Dimens.sm)
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
