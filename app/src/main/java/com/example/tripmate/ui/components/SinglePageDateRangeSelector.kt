package com.example.tripmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tripmate.ui.theme.Dimens
import com.example.tripmate.ui.theme.PrimaryOrange
import com.example.tripmate.ui.theme.SubtleBorder
import com.example.tripmate.ui.theme.TextMuted
import com.example.tripmate.ui.theme.TextPrimary
import com.example.tripmate.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun SinglePageDateRangeSelector(
    startDateMillis: Long,
    endDateMillis: Long,
    onDateRangeSelected: (start: Long, end: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var displayedMonthCal by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                timeInMillis = startDateMillis
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        )
    }

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val daysOfWeek = remember { listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.radiusLg))
            .background(Color.White)
            .padding(vertical = Dimens.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Month Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.sm, vertical = Dimens.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    displayedMonthCal = (displayedMonthCal.clone() as Calendar).apply {
                        add(Calendar.MONTH, -1)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Month",
                    tint = TextPrimary
                )
            }

            Text(
                text = monthYearFormat.format(displayedMonthCal.time),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            IconButton(
                onClick = {
                    displayedMonthCal = (displayedMonthCal.clone() as Calendar).apply {
                        add(Calendar.MONTH, 1)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Month",
                    tint = TextPrimary
                )
            }
        }

        // Days of week row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.sm, vertical = Dimens.xs),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            daysOfWeek.forEach { dayName ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )
                }
            }
        }

        // Calendar Grid
        val cal = (displayedMonthCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0-based for Sunday
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Clean start/end day comparisons (normalize time to midnight)
        val startCal = Calendar.getInstance().apply {
            timeInMillis = startDateMillis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = endDateMillis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7

        for (week in 0 until (totalCells / 7)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.sm, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (dayIndex in 0..6) {
                    val cellIndex = week * 7 + dayIndex
                    val dayOfMonth = cellIndex - firstDayOfWeek + 1

                    if (dayOfMonth in 1..daysInMonth) {
                        val currentCellCal = (displayedMonthCal.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }
                        val cellMillis = currentCellCal.timeInMillis
                        val isStart = currentCellCal.get(Calendar.YEAR) == startCal.get(Calendar.YEAR) &&
                                currentCellCal.get(Calendar.DAY_OF_YEAR) == startCal.get(Calendar.DAY_OF_YEAR)
                        val isEnd = currentCellCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) &&
                                currentCellCal.get(Calendar.DAY_OF_YEAR) == endCal.get(Calendar.DAY_OF_YEAR)
                        val isInRange = cellMillis > startCal.timeInMillis && cellMillis < endCal.timeInMillis

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(Dimens.radiusFull))
                                .background(
                                    when {
                                        isStart || isEnd -> PrimaryOrange
                                        isInRange -> PrimaryOrange.copy(alpha = 0.15f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable {
                                    if (cellMillis < startCal.timeInMillis) {
                                        // Picked an earlier date, make it new start
                                        onDateRangeSelected(cellMillis, endCal.timeInMillis)
                                    } else if (cellMillis == startCal.timeInMillis) {
                                        // Same day, set 1-day trip
                                        onDateRangeSelected(cellMillis, cellMillis)
                                    } else {
                                        // Later date, set as end date
                                        onDateRangeSelected(startCal.timeInMillis, cellMillis)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$dayOfMonth",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isStart || isEnd || isInRange) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isStart || isEnd -> Color.White
                                    isInRange -> PrimaryOrange
                                    else -> TextPrimary
                                }
                            )
                        }
                    } else {
                        // Empty cell
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}
