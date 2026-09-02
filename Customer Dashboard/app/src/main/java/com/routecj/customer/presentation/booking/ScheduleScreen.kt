package com.routecj.customer.presentation.booking

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.routecj.customer.presentation.components.*
import com.routecj.customer.ui.theme.RouteCJSpacing
import com.routecj.customer.ui.theme.StitchTonalBorder
import java.text.SimpleDateFormat
import java.util.*

data class DateOption(
    val dayOfWeek: String,
    val dayNumber: String,
    val fullDate: String,
    val label: String
)

data class TimeSlotOption(
    val title: String,
    val timeRange: String,
    val period: String
)

@Composable
fun ScheduleScreen(
    viewModel: BookingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateNext: () -> Unit
) {
    val schedState by viewModel.scheduleState.collectAsState()
    val pkgState by viewModel.packageState.collectAsState()

    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val numFormat = SimpleDateFormat("dd", Locale.getDefault())
    val fullFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val dateOptions = remember {
        (0..6).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
            val label = when (offset) {
                0 -> "Today"
                1 -> "Tomorrow"
                else -> dayFormat.format(cal.time)
            }
            DateOption(
                dayOfWeek = dayFormat.format(cal.time).uppercase(),
                dayNumber = numFormat.format(cal.time),
                fullDate = fullFormat.format(cal.time),
                label = label
            )
        }
    }

    val timeSlots = listOf(
        TimeSlotOption("Morning", "09:00 AM – 12:00 PM", "9am-12pm"),
        TimeSlotOption("Afternoon", "12:00 PM – 03:00 PM", "12pm-3pm"),
        TimeSlotOption("Evening", "03:00 PM – 06:00 PM", "3pm-6pm"),
        TimeSlotOption("Night", "06:00 PM – 09:00 PM", "6pm-9pm")
    )

    var selectedDateIndex by remember { mutableStateOf(0) }
    var selectedSlot by remember { mutableStateOf(schedState.timeSlot ?: timeSlots.first().timeRange) }
    var instructions by remember { mutableStateOf(pkgState.specialInstructions ?: "") }

    Scaffold(
        topBar = {
            RouteCJTopBar(
                title = "Create Delivery",
                onBackClick = onNavigateBack
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, StitchTonalBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    RouteCJButton(
                        text = "Review Booking",
                        onClick = {
                            val selectedDateObj = dateOptions[selectedDateIndex]
                            viewModel.updateSchedule(
                                date = "${selectedDateObj.label} (${selectedDateObj.dayNumber})",
                                timeSlot = selectedSlot
                            )
                            viewModel.updatePackage(
                                packageType = pkgState.packageType ?: "Small Parcel",
                                itemDescription = pkgState.itemDescription ?: "",
                                packageCount = (pkgState.packageCount ?: 1).toString(),
                                weight = (pkgState.weight ?: 1.0).toString(),
                                specialInstructions = instructions
                            )
                            onNavigateNext()
                        }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = RouteCJSpacing.Default)
                .verticalScroll(rememberScrollState())
        ) {
            RouteCJStepProgressHeader(
                currentStep = 4,
                totalSteps = 4,
                stepTitle = "Slot & Instructions"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Select Date",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                dateOptions.forEachIndexed { index, dateOpt ->
                    val isSelected = selectedDateIndex == index
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        label = "date_card_bg"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else StitchTonalBorder,
                        label = "date_card_border"
                    )

                    Surface(
                        modifier = Modifier
                            .width(72.dp)
                            .height(84.dp)
                            .clickable { selectedDateIndex = index },
                        shape = RoundedCornerShape(16.dp),
                        color = bgColor,
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = dateOpt.dayOfWeek,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dateOpt.dayNumber,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Select Time Slot",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    timeSlots.take(2).forEach { slot ->
                        TimeSlotCard(
                            slot = slot,
                            isSelected = selectedSlot == slot.timeRange,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedSlot = slot.timeRange }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    timeSlots.drop(2).forEach { slot ->
                        TimeSlotCard(
                            slot = slot,
                            isSelected = selectedSlot == slot.timeRange,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedSlot = slot.timeRange }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Delivery Instructions",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${instructions.length}/250",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            RouteCJTextField(
                value = instructions,
                onValueChange = { if (it.length <= 250) instructions = it },
                label = "Special instructions for the driver",
                placeholder = "e.g., Leave with security, gate code #1234, call upon arrival",
                singleLine = false,
                maxLines = 4,
                minLines = 3
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TimeSlotCard(
    slot: TimeSlotOption,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainer,
        label = "slot_card_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else StitchTonalBorder,
        label = "slot_card_border"
    )

    Surface(
        modifier = modifier
            .height(96.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = slot.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            Text(
                text = slot.timeRange,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
