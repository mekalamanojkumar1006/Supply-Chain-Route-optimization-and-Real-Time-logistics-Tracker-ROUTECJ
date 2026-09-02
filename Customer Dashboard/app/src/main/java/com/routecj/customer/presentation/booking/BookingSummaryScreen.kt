package com.routecj.customer.presentation.booking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.routecj.customer.presentation.components.*
import com.routecj.customer.ui.theme.BrandPrimaryBlue
import com.routecj.customer.ui.theme.RouteCJSpacing
import com.routecj.customer.ui.theme.StitchTonalBorder

@Composable
fun BookingSummaryScreen(
    viewModel: BookingViewModel,
    onNavigateBack: () -> Unit,
    onBookingSuccess: (String) -> Unit
) {
    val bookingState by viewModel.bookingState.collectAsState()
    val pickupState by viewModel.locationState.collectAsState()
    val destState by viewModel.destinationState.collectAsState()
    val pkgState by viewModel.packageState.collectAsState()
    val schedState by viewModel.scheduleState.collectAsState()
    val bookingRoutePoints by viewModel.bookingRoutePoints.collectAsState()

    LaunchedEffect(bookingState) {
        if (bookingState is BookingState.Success) {
            onBookingSuccess((bookingState as BookingState.Success).orderId)
        }
    }

    val pickupPair = if (pickupState is LocationState.Success) {
        val loc = pickupState as LocationState.Success
        Pair(loc.latitude, loc.longitude)
    } else null

    val destPair = if (destState is DestinationState.Success) {
        val dest = destState as DestinationState.Success
        Pair(dest.latitude, dest.longitude)
    } else null

    Scaffold(
        topBar = {
            RouteCJTopBar(
                title = "Booking Summary",
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
                        text = "Confirm Booking",
                        onClick = { viewModel.confirmBooking() },
                        isLoading = bookingState is BookingState.Creating
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
            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle Header
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Review Shipment",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Review your delivery details before confirming",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. ROUTE SECTION CARD
            RouteCJCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline)
                        )
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(44.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        )
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Pickup Location",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val pickupAddress = if (pickupState is LocationState.Success) {
                                (pickupState as LocationState.Success).address ?: "Coordinates: ${(pickupState as LocationState.Success).latitude}, ${(pickupState as LocationState.Success).longitude}"
                            } else "Not specified"
                            Text(
                                text = pickupAddress,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column {
                            Text(
                                text = "Delivery Destination",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val destAddress = if (destState is DestinationState.Success) {
                                (destState as DestinationState.Success).address ?: "Coordinates: ${(destState as DestinationState.Success).latitude}, ${(destState as DestinationState.Success).longitude}"
                            } else "Not specified"
                            Text(
                                text = destAddress,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // OpenStreetMap osmdroid Map Preview
            if (pickupPair != null) {
                RouteCJMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    pickupLocation = pickupPair,
                    destinationLocation = destPair,
                    routePoints = bookingRoutePoints,
                    showDriver = false,
                    showRoute = true
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. BENTO DETAILS GRID (2x2)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BentoSummaryCard(
                        title = "Package Type",
                        value = pkgState.packageType ?: "Small Parcel",
                        icon = Icons.Default.Description,
                        modifier = Modifier.weight(1f)
                    )
                    BentoSummaryCard(
                        title = "Total Weight",
                        value = "${pkgState.weight ?: 1.0} kg",
                        icon = Icons.Default.Scale,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BentoSummaryCard(
                        title = "Item Count",
                        value = "${pkgState.packageCount ?: 1} units",
                        icon = Icons.Default.Inventory2,
                        modifier = Modifier.weight(1f)
                    )
                    BentoSummaryCard(
                        title = "Pickup Slot",
                        value = schedState.timeSlot?.split("–")?.firstOrNull()?.trim() ?: "Scheduled",
                        icon = Icons.Default.Schedule,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. INSTRUCTIONS (if provided)
            if (!pkgState.specialInstructions.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                RouteCJCard(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Text(
                        text = "Instructions for Driver",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = pkgState.specialInstructions ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. FARE BREAKDOWN CARD
            RouteCJCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Text(
                    text = "Fare Summary",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Base Fare", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹150.00", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Handling & Service", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹35.00", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("GST (18%)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹33.30", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Estimated Fare",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "₹218.30",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (bookingState is BookingState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                RouteCJErrorState(
                    message = (bookingState as BookingState.Error).message,
                    onRetry = { viewModel.retryBooking() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun BentoSummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, StitchTonalBorder)
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
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

