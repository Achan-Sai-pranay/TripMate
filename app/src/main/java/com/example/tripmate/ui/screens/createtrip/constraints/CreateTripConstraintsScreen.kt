package com.example.tripmate.ui.screens.createtrip.constraints

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tripmate.model.TravelPace
import com.example.tripmate.model.TripConstraints
import com.example.tripmate.model.WalkingTolerance
import com.example.tripmate.ui.theme.Dimens
import java.util.Locale

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CreateTripConstraintsScreen(
    onBackClick: () -> Unit,
    onSkipClick: () -> Unit,
    onGenerateTripClick: (TripConstraints) -> Unit,
    modifier: Modifier = Modifier
) {
    var constraints by remember { mutableStateOf(TripConstraints()) }
    var mustVisitInput by remember { mutableStateOf("") }
    var avoidInput by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ConstraintsTopBar(onBackClick = onBackClick, onSkipClick = onSkipClick)
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .navigationBarsPadding()
                    .padding(Dimens.marginMobile)
            ) {
                Button(
                    onClick = { onGenerateTripClick(constraints) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(Dimens.radiusFull),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        text = "  Generate My Trip",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = Dimens.marginMobile,
                vertical = Dimens.lg
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.xl)
        ) {
            item {
                Column {
                    Text(
                        text = "Set your limits",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tell us how you like to travel. We'll fine-tune the itinerary to match your style.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Dimens.sm)
                    )
                }
            }

            item {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionTitle(icon = Icons.Filled.AccountBalanceWallet, title = "Total Budget")
                    }

                    // Manual text entry — no ceiling, syncs both ways with the slider below
                    var budgetText by remember(constraints.budget) {
                        mutableStateOf(constraints.budget.toString())
                    }
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            budgetText = digits
                            val parsed = digits.toIntOrNull()
                            if (parsed != null) {
                                constraints = constraints.copy(budget = parsed.coerceIn(0, 10_000_000))
                            } else if (digits.isEmpty()) {
                                constraints = constraints.copy(budget = 0)
                            }
                        },
                        leadingIcon = {
                            Text(
                                "₹",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = Dimens.sm)
                            )
                        },
                        label = { Text("Enter amount") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimens.sm),
                        shape = RoundedCornerShape(Dimens.radiusMd)
                    )

                    // Visual slider capped at 50k; text field handles values above that
                    Slider(
                        value = constraints.budget.toFloat().coerceIn(0f, 50_000f),
                        onValueChange = {
                            constraints = constraints.copy(budget = it.toInt())
                            budgetText = it.toInt().toString()
                        },
                        valueRange = 0f..50_000f,
                        steps = 49,
                        modifier = Modifier.padding(top = Dimens.sm),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("₹0", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹50,000+ (type above for more)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }


            item {
                SectionCard {
                    SectionTitle(icon = Icons.Filled.Speed, title = "Travel Pace")
                    Column(
                        modifier = Modifier
                            .padding(top = Dimens.md)
                            .selectableGroup(),
                        verticalArrangement = Arrangement.spacedBy(Dimens.sm)
                    ) {
                        TravelPace.entries.forEach { pace ->
                            TravelPaceRow(
                                pace = pace,
                                isSelected = constraints.travelPace == pace,
                                onSelect = { constraints = constraints.copy(travelPace = pace) }
                            )
                        }
                    }
                }
            }

            item {
                SectionCard {
                    SectionTitle(icon = Icons.Filled.DirectionsWalk, title = "Walking Tolerance")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimens.md),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                    ) {
                        WalkingTolerance.entries.forEach { tolerance ->
                            WalkingToleranceButton(
                                tolerance = tolerance,
                                isSelected = constraints.walkingTolerance == tolerance,
                                onClick = { constraints = constraints.copy(walkingTolerance = tolerance) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                SectionCard {
                    TagInputSection(
                        icon = Icons.Filled.ThumbUp,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        title = "Must Visit",
                        placeholder = "e.g. Local markets, museums",
                        inputValue = mustVisitInput,
                        onInputChange = { mustVisitInput = it },
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        tags = constraints.mustVisitTags,
                        onAddTag = {
                            if (mustVisitInput.isNotBlank()) {
                                constraints = constraints.copy(
                                    mustVisitTags = constraints.mustVisitTags + mustVisitInput.trim()
                                )
                                mustVisitInput = ""
                            }
                        },
                        onRemoveTag = { tag ->
                            constraints = constraints.copy(mustVisitTags = constraints.mustVisitTags - tag)
                        }
                    )
                    Column(modifier = Modifier.padding(top = Dimens.lg)) {
                        TagInputSection(
                            icon = Icons.Filled.ThumbDown,
                            iconTint = MaterialTheme.colorScheme.error,
                            title = "Avoid",
                            placeholder = "e.g. Tourist traps, heights",
                            inputValue = avoidInput,
                            onInputChange = { avoidInput = it },
                            accentColor = MaterialTheme.colorScheme.error,
                            tags = constraints.avoidTags,
                            onAddTag = {
                                if (avoidInput.isNotBlank()) {
                                    constraints = constraints.copy(
                                        avoidTags = constraints.avoidTags + avoidInput.trim()
                                    )
                                    avoidInput = ""
                                }
                            },
                            onRemoveTag = { tag ->
                                constraints = constraints.copy(avoidTags = constraints.avoidTags - tag)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConstraintsTopBar(
    onBackClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Dimens.marginMobile, vertical = Dimens.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Go back",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "STEP 3 OF ${com.example.tripmate.util.Features.TOTAL_WIZARD_STEPS}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        TextButton(onClick = onSkipClick) {
            Text(
                text = "Skip",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SectionCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(Dimens.lg), content = content)
    }
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = Dimens.sm)
        )
    }
}

@Composable
private fun ConstraintPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(Dimens.radiusFull),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Dimens.md, vertical = Dimens.xs)
        )
    }
}

@Composable
private fun TravelPaceRow(
    pace: TravelPace,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.radiusMd))
            .selectable(selected = isSelected, onClick = onSelect, role = androidx.compose.ui.semantics.Role.RadioButton)
            .background(
                if (isSelected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceBright
            )
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(Dimens.radiusMd)
            )
            .padding(Dimens.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
        Text(
            text = pace.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = Dimens.xs)
        )
    }
}

@Composable
private fun WalkingToleranceButton(
    tolerance: WalkingTolerance,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.radiusMd),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Text(
            text = tolerance.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.sm)
        )
    }
}

@Composable
private fun TagInputSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    placeholder: String,
    inputValue: String,
    onInputChange: (String) -> Unit,
    accentColor: Color,
    tags: List<String>,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit
) {
    Column {
        SectionTitle(icon = icon, title = title)
        Box(modifier = Modifier.padding(top = Dimens.sm)) {
            OutlinedTextField(
                value = inputValue,
                onValueChange = onInputChange,
                placeholder = {
                    Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default,
                trailingIcon = {
                    IconButton(onClick = onAddTag) {
                        Icon(Icons.Filled.Add, contentDescription = "Add", tint = accentColor)
                    }
                },
                shape = RoundedCornerShape(Dimens.radiusMd),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceBright
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (tags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = Dimens.sm),
                horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
                verticalArrangement = Arrangement.spacedBy(Dimens.xs)
            ) {
                tags.forEach { tag ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimens.radiusFull))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(start = Dimens.sm, end = 6.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = { onRemoveTag(tag) },
                            modifier = Modifier.size(20.dp).padding(start = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove $tag",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
