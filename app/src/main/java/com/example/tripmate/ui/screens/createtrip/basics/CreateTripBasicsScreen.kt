package com.example.tripmate.ui.screens.createtrip.basics

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tripmate.model.TravelerOption
import com.example.tripmate.ui.components.SinglePageDateRangeSelector
import com.example.tripmate.ui.components.SmartDestinationSearchField
import com.example.tripmate.ui.components.StepProgressBar
import com.example.tripmate.ui.components.WizardTopBar
import com.example.tripmate.ui.theme.Dimens
import com.example.tripmate.ui.theme.PrimaryOrange
import com.example.tripmate.ui.theme.SubtleBorder
import com.example.tripmate.ui.theme.TextMuted
import com.example.tripmate.ui.theme.TextPrimary
import com.example.tripmate.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class TripBasics(
    val destination: String,
    val startDateMillis: Long,
    val endDateMillis: Long,
    val travelers: TravelerOption
)

@Composable
fun CreateTripBasicsScreen(
    initialDestination: String = "",
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    onNextClick: (TripBasics) -> Unit,
    modifier: Modifier = Modifier
) {
    var destination by remember { mutableStateOf(initialDestination) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val defaultEnd = remember {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 3)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    var startDateMillis by remember { mutableStateOf(today) }
    var endDateMillis by remember { mutableStateOf(defaultEnd) }
    var selectedTraveler by remember { mutableStateOf(TravelerOption.JUST_ME) }

    val isFormValid = destination.isNotBlank() && endDateMillis >= startDateMillis
    val scrollState = rememberScrollState()

    // Header gradient subtle sunset glow
    val headerGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFEBE5).copy(alpha = 0.6f),
            Color(0xFFFAFAFA)
        )
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            WizardTopBar(onBackClick = onBackClick, onCloseClick = onCloseClick)
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.marginMobile, vertical = Dimens.md)
                ) {
                    Button(
                        onClick = {
                            onNextClick(
                                TripBasics(
                                    destination = destination,
                                    startDateMillis = startDateMillis,
                                    endDateMillis = endDateMillis,
                                    travelers = selectedTraveler
                                )
                            )
                        },
                        enabled = isFormValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(Dimens.radiusFull),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryOrange,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFE2E8F0),
                            disabledContentColor = TextMuted
                        )
                    ) {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(Dimens.xs))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            // Top Section with gradient & title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGradient)
                    .padding(horizontal = Dimens.marginMobile, vertical = Dimens.md)
            ) {
                Column {
                    StepProgressBar(
                        currentStep = 1,
                        totalSteps = com.example.tripmate.util.Features.TOTAL_WIZARD_STEPS,
                        modifier = Modifier.padding(bottom = Dimens.md)
                    )
                    Text(
                        text = "Where are you going?",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "Pick your destination and dates in a snap.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.marginMobile, vertical = Dimens.md),
                verticalArrangement = Arrangement.spacedBy(Dimens.lg)
            ) {
                // Section 1: Predictive Smart Destination Search
                Column {
                    Text(
                        text = "DESTINATION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = Dimens.xs)
                    )
                    SmartDestinationSearchField(
                        query = destination,
                        onQueryChange = { destination = it },
                        onDestinationSelected = { suggestion ->
                            val dest = "${suggestion.name}, ${suggestion.country}"
                            destination = dest
                            coroutineScope.launch {
                                com.example.tripmate.data.WikipediaImageService.imageForDestination(dest)
                            }
                        },
                        placeholder = "Search destination (e.g. Goa, Paris, Manali)"
                    )
                }

                // Section 2: Single-Page Interactive Date Range Selector (matching Screenshot 3)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SELECT DATES",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        // Range chip badge
                        Surface(
                            shape = RoundedCornerShape(Dimens.radiusFull),
                            color = PrimaryOrange.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "${dateFormat.format(startDateMillis)} – ${dateFormat.format(endDateMillis)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryOrange,
                                modifier = Modifier.padding(horizontal = Dimens.sm, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimens.xs))

                    Card(
                        shape = RoundedCornerShape(Dimens.radiusLg),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SubtleBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SinglePageDateRangeSelector(
                            startDateMillis = startDateMillis,
                            endDateMillis = endDateMillis,
                            onDateRangeSelected = { start, end ->
                                startDateMillis = start
                                endDateMillis = end
                            },
                            modifier = Modifier.padding(Dimens.xs)
                        )
                    }
                }

                // Section 3: Travelers Selection
                Column(modifier = Modifier.padding(bottom = Dimens.xl)) {
                    Text(
                        text = "WHO'S TRAVELING?",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = Dimens.sm)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                    ) {
                        items(TravelerOption.entries) { option ->
                            val isSelected = option == selectedTraveler
                            Card(
                                onClick = { selectedTraveler = option },
                                shape = RoundedCornerShape(Dimens.radiusFull),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PrimaryOrange else Color.White
                                ),
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, SubtleBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = Dimens.md, vertical = Dimens.sm),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextPrimary
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
