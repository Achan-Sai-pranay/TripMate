package com.example.tripmate.ui.screens.createtrip.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.tripmate.model.TripPreferences
import com.example.tripmate.ui.theme.Dimens
import com.example.tripmate.util.Features
import kotlinx.coroutines.launch
import java.util.Locale

private const val TOTAL_CARDS = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripPreferencesScreen(
    onBackClick: () -> Unit,
    onDoneClick: (TripPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    var prefs by remember { mutableStateOf(TripPreferences()) }
    val pagerState = rememberPagerState(pageCount = { TOTAL_CARDS })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = Dimens.marginMobile, vertical = Dimens.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (pagerState.currentPage == 0) onBackClick()
                    else coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    "STEP 2 OF ${Features.TOTAL_WIZARD_STEPS}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                TextButton(onClick = { onDoneClick(prefs) }) {
                    Text("Skip", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(Dimens.marginMobile)
            ) {
                CardDots(total = TOTAL_CARDS, current = pagerState.currentPage, modifier = Modifier.padding(bottom = Dimens.md))
                Button(
                    onClick = {
                        val isLastCard = pagerState.currentPage == TOTAL_CARDS - 1
                        if (isLastCard) {
                            onDoneClick(prefs)
                        } else {
                            coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(Dimens.radiusFull),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    val isLastCard = pagerState.currentPage == TOTAL_CARDS - 1
                    Text(if (isLastCard) "Continue" else "Next", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (!isLastCard) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = Dimens.xs).height(18.dp))
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            userScrollEnabled = true
        ) { page ->
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = Dimens.marginMobile),
                verticalArrangement = Arrangement.Center
            ) {
                when (page) {
                    0 -> DailyScheduleQuestionCard(
                        startTime = prefs.exploreStartTime,
                        onStartTimeChange = { prefs = prefs.copy(exploreStartTime = it) },
                        endTime = prefs.dayEndTime,
                        onEndTimeChange = { prefs = prefs.copy(dayEndTime = it) }
                    )
                    1 -> TagQuestionCard(
                        question = "Any fixed-time activities or reservations?",
                        placeholder = "e.g. Dinner reservation 7 PM, Train at 4 PM",
                        tags = prefs.fixedActivities,
                        onAddTag = { tag -> prefs = prefs.copy(fixedActivities = prefs.fixedActivities + tag) },
                        onRemoveTag = { tag -> prefs = prefs.copy(fixedActivities = prefs.fixedActivities - tag) }
                    )
                    2 -> ToggleQuestionCard(
                        question = "Prioritize famous places even if they need an early start or extra travel?",
                        subtitle = "Off means I'll favor convenient, nearby options instead.",
                        value = prefs.prioritizeFamousPlaces,
                        onValueChange = { prefs = prefs.copy(prioritizeFamousPlaces = it) }
                    )
                    3 -> ChipsQuestionCard(
                        question = "Any food preferences or restrictions?",
                        options = listOf("Vegetarian", "Vegan", "Halal", "Jain", "No Beef", "No Seafood", "No Restrictions"),
                        selected = prefs.foodPreferences,
                        onToggle = { option ->
                            prefs = prefs.copy(
                                foodPreferences = if (option in prefs.foodPreferences) prefs.foodPreferences - option
                                else prefs.foodPreferences + option
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CardDots(total: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (index == current) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
            )
        }
    }
}

@Composable
private fun QuestionHeader(question: String, subtitle: String? = null) {
    Text(
        text = question,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    subtitle?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = Dimens.sm)
        )
    }
}

@Composable
private fun DailyScheduleQuestionCard(
    startTime: String,
    onStartTimeChange: (String) -> Unit,
    endTime: String,
    onEndTimeChange: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        QuestionHeader(
            question = "Daily Schedule",
            subtitle = "Set your ideal window for exploring each day."
        )
        Spacer(modifier = Modifier.height(Dimens.lg))
        TimeSelectionCard(
            label = "Start Exploring",
            value = startTime,
            onValueChange = onStartTimeChange
        )
        Spacer(modifier = Modifier.height(Dimens.md))
        TimeSelectionCard(
            label = "Wrap Up Day",
            value = endTime,
            onValueChange = onEndTimeChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSelectionCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    Card(
        onClick = { showPicker = true },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.lg, horizontal = Dimens.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Dimens.xs)
            )
        }
    }

    if (showPicker) {
        val (initHour, initMinute) = remember(value) { parseTimeString(value) }
        val timeState = rememberTimePickerState(
            initialHour = initHour,
            initialMinute = initMinute,
            is24Hour = false
        )
        Dialog(onDismissRequest = { showPicker = false }) {
            Card(shape = RoundedCornerShape(Dimens.radiusCard)) {
                Column(modifier = Modifier.padding(Dimens.lg), horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timeState)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = Dimens.md), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showPicker = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            val hour12 = if (timeState.hour % 12 == 0) 12 else timeState.hour % 12
                            val amPm = if (timeState.hour >= 12) "PM" else "AM"
                            onValueChange(String.format(Locale.getDefault(), "%02d:%02d %s", hour12, timeState.minute, amPm))
                            showPicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}

private fun parseTimeString(timeStr: String): Pair<Int, Int> {
    return try {
        val parts = timeStr.trim().split(" ")
        val timeParts = parts[0].split(":")
        var h = timeParts[0].toInt()
        val m = timeParts[1].toInt()
        val amPm = parts.getOrNull(1)?.uppercase() ?: "AM"
        if (amPm == "PM" && h < 12) h += 12
        if (amPm == "AM" && h == 12) h = 0
        h to m
    } catch (_: Exception) {
        9 to 0
    }
}

@Composable
private fun ToggleQuestionCard(question: String, subtitle: String, value: Boolean, onValueChange: (Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        QuestionHeader(question, subtitle)
        Row(
            modifier = Modifier
                .padding(top = Dimens.xl)
                .clip(RoundedCornerShape(Dimens.radiusFull))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(Dimens.radiusFull))
                .padding(horizontal = Dimens.lg, vertical = Dimens.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (value) "Yes, prioritize famous" else "No, prioritize convenient", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Switch(
                checked = value,
                onCheckedChange = onValueChange,
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(start = Dimens.md)
            )
        }
    }
}

@Composable
private fun TagQuestionCard(
    question: String,
    placeholder: String,
    tags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        QuestionHeader(question)
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = {
                    if (input.isNotBlank()) { onAddTag(input.trim()); input = "" }
                }) { Icon(Icons.Filled.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary) }
            },
            shape = RoundedCornerShape(Dimens.radiusMd),
            modifier = Modifier.fillMaxWidth().padding(top = Dimens.xl)
        )
        if (tags.isEmpty()) {
            Text(
                text = "None added — that's fine too.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.md)
            )
        } else {
            FlowRow(modifier = Modifier.padding(top = Dimens.md), horizontalArrangement = Arrangement.spacedBy(Dimens.xs), verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                tags.forEach { tag ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimens.radiusFull))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(start = Dimens.sm, end = 6.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Text(tag, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = { onRemoveTag(tag) }, modifier = Modifier.size(20.dp).padding(start = 2.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipsQuestionCard(
    question: String,
    options: List<String>,
    selected: List<String>,
    onToggle: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        QuestionHeader(question)
        FlowRow(
            modifier = Modifier.padding(top = Dimens.xl),
            horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
            verticalArrangement = Arrangement.spacedBy(Dimens.sm)
        ) {
            options.forEach { option ->
                val isSelected = option in selected
                Card(
                    onClick = { onToggle(option) },
                    shape = RoundedCornerShape(Dimens.radiusFull),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Dimens.md, vertical = Dimens.sm)
                    )
                }
            }
        }
    }
}
