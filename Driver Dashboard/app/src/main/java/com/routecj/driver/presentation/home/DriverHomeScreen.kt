package com.routecj.driver.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.routecj.driver.domain.model.Driver
import com.routecj.driver.domain.model.DriverAssignment
import com.routecj.driver.domain.model.Vehicle
import com.routecj.driver.domain.usecase.DriverHomeData
import com.routecj.driver.presentation.components.BadgeType
import com.routecj.driver.presentation.components.ErrorState
import com.routecj.driver.presentation.components.LoadingState
import com.routecj.driver.presentation.components.RouteCJButton
import com.routecj.driver.presentation.components.RouteCJCard
import com.routecj.driver.presentation.components.StatusBadge
import com.routecj.driver.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverHomeScreen(
    driver: Driver,
    driverHomeViewModel: DriverHomeViewModel,
    notificationViewModel: com.routecj.driver.presentation.notification.NotificationViewModel,
    onNavigateToTrip: (String) -> Unit,
    onNavigateToBookedSlots: () -> Unit,
    onNavigateToTripHistory: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToVehicleDetails: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by driverHomeViewModel.uiState.collectAsState()
    val unreadBadgeCount by notificationViewModel.unreadBadgeCount.collectAsState()

    LaunchedEffect(driver.id) {
        driverHomeViewModel.initialize(driver)
        notificationViewModel.initialize(driver.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.clickable { onNavigateToProfile() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(RouteCJCyan, RouteCJBlue))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "RouteCJ Driver",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "ID: ${driver.id.ifBlank { "DRV" }}",
                                color = RouteCJCyanLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    // Notification Bell Icon with real Unread Badge
                    IconButton(onClick = onNavigateToNotifications) {
                        BadgedBox(
                            badge = {
                                if (unreadBadgeCount > 0) {
                                    Badge(
                                        containerColor = RouteCJError,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = if (unreadBadgeCount > 99) "99+" else "$unreadBadgeCount",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = if (unreadBadgeCount > 0) RouteCJCyanLight else RouteCJTextSecondaryDark
                            )
                        }
                    }

                    // Profile Icon
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Driver Profile",
                            tint = RouteCJCyanLight
                        )
                    }

                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = RouteCJTextSecondaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RouteCJNavySurface
                )
            )
        },
        containerColor = RouteCJNavyDark
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(RouteCJNavyDark)
        ) {
            when (val state = uiState) {
                is DriverHomeUiState.Loading -> {
                    LoadingState(message = "Loading Driver Assignments...", modifier = Modifier.fillMaxSize())
                }

                is DriverHomeUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        actionText = "RETRY",
                        onAction = { driverHomeViewModel.retry() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is DriverHomeUiState.Success -> {
                    DriverHomeContent(
                        data = state.data,
                        onNavigateToTrip = onNavigateToTrip,
                        onNavigateToBookedSlots = onNavigateToBookedSlots,
                        onNavigateToTripHistory = onNavigateToTripHistory,
                        onNavigateToProfile = onNavigateToProfile,
                        onNavigateToVehicleDetails = onNavigateToVehicleDetails,
                        onRefresh = { driverHomeViewModel.retry() }
                    )
                }
            }
        }
    }
}

@Composable
fun DriverHomeContent(
    data: DriverHomeData,
    onNavigateToTrip: (String) -> Unit,
    onNavigateToBookedSlots: () -> Unit,
    onNavigateToTripHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToVehicleDetails: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Driver Greeting & Status Card (Clickable to Profile)
        DriverHeaderCard(
            driver = data.driver,
            onClick = onNavigateToProfile
        )

        // Summary Metric Row
        DriverSummaryRow(summary = data.summary)

        // Booked Pickups / Slots Banner Card
        BookedSlotsBannerCard(
            count = data.bookedPickupsCount,
            onViewSlots = onNavigateToBookedSlots
        )

        // Trip History Banner Card
        TripHistoryBannerCard(
            completedCount = data.summary.completedDeliveries,
            onViewHistory = onNavigateToTripHistory
        )

        // Primary Today's Trip Card
        if (data.todayAssignment != null) {
            TodaysTripCard(
                assignment = data.todayAssignment,
                vehicle = data.vehicle,
                onViewTrip = { onNavigateToTrip(data.todayAssignment.id) }
            )
        } else {
            EmptyAssignmentCard(onRefresh = onRefresh)
        }

        // Next Assignment (if exists)
        if (data.nextAssignment != null) {
            NextAssignmentCard(
                assignment = data.nextAssignment,
                onViewTrip = { onNavigateToTrip(data.nextAssignment.id) }
            )
        }

        // Assigned Vehicle Card (Clickable to Vehicle Details)
        AssignedVehicleCard(
            vehicle = data.vehicle,
            hasAssignedVehicle = data.hasAssignedVehicle,
            onClick = onNavigateToVehicleDetails
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun TripHistoryBannerCard(
    completedCount: Int,
    onViewHistory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(RouteCJSuccess.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = RouteCJSuccess,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "TRIP HISTORY",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "$completedCount completed trip${if (completedCount == 1) "" else "s"}",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 12.sp
                    )
                }
            }

            Button(
                onClick = onViewHistory,
                colors = ButtonDefaults.buttonColors(containerColor = RouteCJBlue),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("VIEW ALL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BookedSlotsBannerCard(
    count: Int,
    onViewSlots: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(RouteCJCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = RouteCJCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "BOOKED PICKUPS",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (count > 0) "$count scheduled pickup${if (count > 1) "s" else ""} ready" else "No pending customer pickups",
                        color = if (count > 0) RouteCJCyanLight else RouteCJTextSecondaryDark,
                        fontSize = 12.sp
                    )
                }
            }

            Button(
                onClick = onViewSlots,
                colors = ButtonDefaults.buttonColors(containerColor = RouteCJBlue),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("VIEW SLOTS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DriverHeaderCard(
    driver: Driver,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(RouteCJCyan, RouteCJBlue))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = driver.name.take(1).ifBlank { "D" }.uppercase(),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Welcome Back,",
                    color = RouteCJTextSecondaryDark,
                    fontSize = 12.sp
                )
                Text(
                    text = driver.name.ifBlank { "Authorized Driver" },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Driver Status Badge (Read-Only)
            Surface(
                color = when (driver.status.name.uppercase()) {
                    "AVAILABLE" -> RouteCJSuccess.copy(alpha = 0.18f)
                    "ON_DUTY", "BUSY" -> RouteCJWarning.copy(alpha = 0.18f)
                    else -> RouteCJNavyCard
                },
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                when (driver.status.name.uppercase()) {
                                    "AVAILABLE" -> RouteCJSuccess
                                    "ON_DUTY", "BUSY" -> RouteCJWarning
                                    else -> RouteCJTextSecondaryDark
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = driver.status.name,
                        color = when (driver.status.name.uppercase()) {
                            "AVAILABLE" -> RouteCJSuccess
                            "ON_DUTY", "BUSY" -> RouteCJWarning
                            else -> RouteCJTextSecondaryDark
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DriverSummaryRow(summary: com.routecj.driver.domain.model.DriverSummaryMetrics) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryStatBox(
            title = "Assigned",
            value = "${summary.totalAssigned}",
            icon = Icons.AutoMirrored.Filled.Assignment,
            iconTint = RouteCJCyan,
            modifier = Modifier.weight(1f)
        )
        SummaryStatBox(
            title = "Active",
            value = "${summary.activeTrips}",
            icon = Icons.Default.DirectionsCar,
            iconTint = RouteCJWarning,
            modifier = Modifier.weight(1f)
        )
        SummaryStatBox(
            title = "Completed",
            value = "${summary.completedDeliveries}",
            icon = Icons.Default.CheckCircle,
            iconTint = RouteCJSuccess,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SummaryStatBox(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = RouteCJTextSecondaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TodaysTripCard(
    assignment: DriverAssignment,
    vehicle: Vehicle?,
    onViewTrip: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = RouteCJCyanLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TODAY'S TRIP",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    color = RouteCJBlue.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = assignment.status.replace("_", " "),
                        color = RouteCJCyanLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = RouteCJNavyCard, thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Order: ${assignment.orderNumber}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Customer: ${assignment.customerName}",
                color = RouteCJTextSecondaryDark,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Route Points
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(RouteCJCyan)
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(28.dp)
                            .background(RouteCJNavyCard)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(RouteCJSuccess)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Pickup",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 11.sp
                    )
                    Text(
                        text = assignment.pickupLocation,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Destination",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 11.sp
                    )
                    Text(
                        text = assignment.deliveryLocation,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vehicle Metadata
            val vehicleReg = if (vehicle != null) {
                vehicle.registrationNumber.takeIf { it.isNotBlank() }
                    ?: vehicle.vehicleNumber.takeIf { it.isNotBlank() }
                    ?: "Unknown Registration"
            } else {
                "VEHICLE NOT ASSIGNED"
            }

            Surface(
                color = RouteCJNavyDark,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = RouteCJTextSecondaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vehicle: $vehicleReg",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onViewTrip,
                colors = ButtonDefaults.buttonColors(containerColor = RouteCJBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "VIEW TRIP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun NextAssignmentCard(
    assignment: DriverAssignment,
    onViewTrip: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NEXT ASSIGNMENT",
                    color = RouteCJCyanLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = assignment.status.replace("_", " "),
                    color = RouteCJTextSecondaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Order: ${assignment.orderNumber}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${assignment.pickupLocation} → ${assignment.deliveryLocation}",
                color = RouteCJTextSecondaryDark,
                fontSize = 12.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = onViewTrip,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "View Details",
                    color = RouteCJCyanLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = RouteCJCyanLight,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyAssignmentCard(onRefresh: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                tint = RouteCJCyan.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "NO ACTIVE ASSIGNMENT",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "You currently have no assigned deliveries.",
                color = RouteCJTextSecondaryDark,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onRefresh,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RouteCJCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("REFRESH", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AssignedVehicleCard(
    vehicle: Vehicle?,
    hasAssignedVehicle: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = vehicle != null, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Vehicle",
                        tint = RouteCJCyanLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MY VEHICLE",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                if (vehicle != null) {
                    Surface(
                        color = RouteCJSuccess.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = vehicle.status.name,
                            color = RouteCJSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (vehicle != null) {
                Text(
                    text = vehicle.registrationNumber.ifBlank { vehicle.vehicleNumber },
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Type: ${vehicle.vehicleType.name} • Capacity: ${vehicle.capacity} ${vehicle.capacityUnit}",
                    color = RouteCJTextSecondaryDark,
                    fontSize = 12.sp
                )
            } else {
                Text(
                    text = if (hasAssignedVehicle) "VEHICLE NOT FOUND" else "VEHICLE NOT ASSIGNED",
                    color = RouteCJError,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (hasAssignedVehicle) "Your assigned vehicle could not be found. Contact Fleet Dispatch." else "You do not have an active vehicle assignment.",
                    color = RouteCJTextSecondaryDark.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
