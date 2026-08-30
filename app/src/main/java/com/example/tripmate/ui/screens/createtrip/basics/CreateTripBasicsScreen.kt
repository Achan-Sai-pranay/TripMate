package com.example.tripmate.ui.screens.createtrip.basics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tripmate.model.TravelerOption
import com.example.tripmate.ui.components.StepProgressBar
import com.example.tripmate.ui.components.WizardTopBar
import com.example.tripmate.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class TripBasics(
    val destination: String,
    val startDateMillis: Long,
    val endDateMillis: Long,
    val travelers: TravelerOption
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripBasicsScreen(
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    onNextClick: (TripBasics) -> Unit,
    modifier: Modifier = Modifier
) {
    var destination by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }
    val today = remember { Calendar.getInstance().timeInMillis }
    val defaultEnd = remember {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 3) }.timeInMillis
    }

    var startDateMillis by remember { mutableStateOf(today) }
    var endDateMillis by remember { mutableStateOf(defaultEnd) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    var selectedTraveler by remember { mutableStateOf(TravelerOption.JUST_ME) }

    // "Next" is only enabled once a destination is entered and the date
    // range is valid.
    val isFormValid = destination.isNotBlank() && endDateMillis >= startDateMillis

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            WizardTopBar(onBackClick = onBackClick, onCloseClick = onCloseClick)
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(Dimens.marginMobile)
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
                        .height(52.dp),
                    shape = RoundedCornerShape(Dimens.radiusFull),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = "Next",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(start = Dimens.xs).height(18.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimens.marginMobile),
            verticalArrangement = Arrangement.Center
        ) {
            Column(modifier = Modifier.padding(bottom = Dimens.xl)) {
                StepProgressBar(currentStep = 1, totalSteps = 6, modifier = Modifier.padding(bottom = Dimens.md))
                Text(
                    text = "Start your next adventure",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Where to, and when? Let's sketch out the basics.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(Dimens.lg)) {
                FormFieldCard(label = "Destination") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(end = Dimens.sm)
                        )
                        BareTextField(
                            value = destination,
                            onValueChange = { destination = it },
                            placeholder = "e.g., Hyderabad, India",
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.md)) {
                    FormFieldCard(
                        label = "Start Date",
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = { showStartPicker = true })
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(end = Dimens.sm).height(18.dp)
                            )
                            Text(text = dateFormat.format(startDateMillis), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    FormFieldCard(
                        label = "End Date",
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = { showEndPicker = true })
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(end = Dimens.sm).height(18.dp)
                            )
                            Text(text = dateFormat.format(endDateMillis), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (endDateMillis < startDateMillis) {
                    Text(
                        text = "End date can't be before the start date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Column {
                    Text(
                        text = "Who's going?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Dimens.sm)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                        items(TravelerOption.entries) { option ->
                            TravelerChip(
                                option = option,
                                isSelected = option == selectedTraveler,
                                onClick = { selectedTraveler = option }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDateMillis)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { startDateMillis = it }
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(28.dp)
        ) {
            DatePicker(state = state)
        }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDateMillis)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { endDateMillis = it }
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(28.dp)
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun FormFieldCard(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusMd),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(Dimens.md)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            content()
        }
    }
}

@Composable
private fun BareTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: androidx.compose.ui.text.TextStyle
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, style = textStyle, color = MaterialTheme.colorScheme.surfaceVariant) },
        singleLine = true,
        textStyle = textStyle,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TravelerChip(
    option: TravelerOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(Dimens.radiusFull),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = Dimens.lg, vertical = Dimens.sm)
        )
    }
}
