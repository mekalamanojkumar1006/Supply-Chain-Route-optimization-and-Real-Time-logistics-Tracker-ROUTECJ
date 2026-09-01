package com.routecj.driver.presentation.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.routecj.driver.domain.model.TripDetails
import com.routecj.driver.presentation.components.BadgeType
import com.routecj.driver.presentation.components.ErrorState
import com.routecj.driver.presentation.components.LoadingState
import com.routecj.driver.presentation.components.RouteCJButton
import com.routecj.driver.presentation.components.RouteCJCard
import com.routecj.driver.presentation.components.StatusBadge
import com.routecj.driver.ui.theme.*
import org.osmdroid.util.GeoPoint

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.routecj.driver.service.DriverGpsState
import com.routecj.driver.service.DriverLocationService
import com.routecj.driver.service.DriverLocationStateHolder
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailsScreen(
    tripId: String,
    driverId: String,
    tripViewModel: TripViewModel,
    onBack: () -> Unit,
    onOpenMap: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by tripViewModel.uiState.collectAsState()
    val gpsState by DriverLocationStateHolder.gpsState.collectAsState()

    // Permission launcher for location
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            val trip = (uiState as? TripDetailsUiState.Success)?.trip
            if (trip != null && isTripActive(trip.status)) {
                DriverLocationService.start(
                    context = context,
                    driverId = driverId,
                    tripId = trip.tripId,
                    orderNumber = trip.orderNumber
                )
            }
        } else {
            DriverLocationStateHolder.updateState(DriverGpsState.PermissionRequired)
        }
    }

    LaunchedEffect(tripId, driverId) {
        tripViewModel.loadTrip(tripId, driverId)
    }

    // Automatically manage Foreground Service based on trip status
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is TripDetailsUiState.Success) {
            val trip = state.trip
            val currentGps = DriverLocationStateHolder.gpsState.value
            
            if (isTripActive(trip.status)) {
                // Only start if not already active/connecting
                if (currentGps == DriverGpsState.Inactive || currentGps == DriverGpsState.StartFailed) {
                    val hasFine = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    val hasCoarse = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasFine || hasCoarse) {
                        DriverLocationService.start(
                            context = context,
                            driverId = driverId,
                            tripId = trip.tripId,
                            orderNumber = trip.orderNumber
                        )
                    } else {
                        DriverLocationStateHolder.updateState(DriverGpsState.PermissionRequired)
                    }
                }
            } else if (trip.status.uppercase() in listOf("DELIVERED", "CANCELLED", "COMPLETED")) {
                if (currentGps != DriverGpsState.Inactive) {
                    DriverLocationService.stop(context)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Trip Details",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
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
                is TripDetailsUiState.Loading -> {
                    LoadingState(message = "Loading Trip Details...", modifier = Modifier.fillMaxSize())
                }

                is TripDetailsUiState.AccessDenied -> {
                    ErrorState(
                        message = "TRIP ACCESS DENIED\n${state.message}",
                        actionText = "BACK TO HOME",
                        onAction = onBack,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is TripDetailsUiState.Error -> {
                    ErrorState(
                        message = "TRIP UNAVAILABLE\n${state.message}",
                        actionText = "RETRY",
                        onAction = { tripViewModel.retry() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is TripDetailsUiState.Success -> {
                    val errorMessage = state.errorMessage
                    LaunchedEffect(errorMessage) {
                        if (errorMessage != null) {
                            android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
                            tripViewModel.clearErrorMessage()
                        }
                    }

                    TripDetailsContent(
                        trip = state.trip,
                        gpsState = gpsState,
                        isActionInProgress = state.isActionInProgress,
                        onOpenMap = { onOpenMap(state.trip.tripId) },
                        onRequestLocationPermission = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        onOpenLocationSettings = {
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        },
                        onStartTrip = {
                            // Check permission before starting trip
                            val hasFine = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (!hasFine && !hasCoarse) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                            tripViewModel.startTrip()
                        },
                        onCompleteTrip = {
                            tripViewModel.completeTrip()
                        }
                    )
                }
            }
        }
    }
}

private fun isTripActive(status: String): Boolean {
    return status.uppercase() in listOf("TRIP_STARTED", "IN_TRANSIT", "DISPATCHED")
}

@Composable
fun TripDetailsContent(
    trip: TripDetails,
    gpsState: DriverGpsState,
    isActionInProgress: Boolean,
    onOpenMap: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onStartTrip: () -> Unit,
    onCompleteTrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showDistanceWarning by remember { mutableStateOf(false) }

    if (showDistanceWarning) {
        AlertDialog(
            onDismissRequest = { showDistanceWarning = false },
            title = { Text("DESTINATION NOT REACHED", color = RouteCJError, fontWeight = FontWeight.Bold) },
            text = {
                val point = gpsState as? DriverGpsState.Active
                val destPoint = GeoPoint(trip.destinationLat, trip.destinationLng)
                val distance = if (point != null) GeoPoint(point.latitude, point.longitude).distanceToAsDouble(destPoint) else 0.0
                val distStr = if (distance < 1000) "${distance.toInt()} m" else String.format("%.1f km", distance / 1000.0)
                Text("You are approximately $distStr from the destination. Are you sure you want to complete the delivery?", color = Color.White)
            },
            confirmButton = {
                TextButton(onClick = {
                    showDistanceWarning = false
                    onCompleteTrip()
                }) {
                    Text("CONTINUE", color = RouteCJSuccess, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDistanceWarning = false }) {
                    Text("CANCEL", color = RouteCJTextSecondaryDark)
                }
            },
            containerColor = RouteCJNavySurface
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Identification Card
        TripHeaderCard(trip = trip)

        // Live GPS Tracking Card (During Active Trips)
        if (isTripActive(trip.status)) {
            TripLiveLocationCard(
                tripStatus = trip.status,
                gpsState = gpsState,
                onRequestPermission = onRequestLocationPermission,
                onOpenLocationSettings = onOpenLocationSettings
            )

            // Open Map & Navigation Button
            Button(
                onClick = onOpenMap,
                colors = ButtonDefaults.buttonColors(containerColor = RouteCJCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = RouteCJNavyDark,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OPEN LIVE MAP & NAVIGATION",
                    color = RouteCJNavyDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Status Timeline
        TripTimelineCard(status = trip.status)

        // Route Card
        TripRouteCard(trip = trip)

        // Parcel Specification Card
        TripParcelCard(trip = trip)

        // Vehicle Info Card
        TripVehicleCard(trip = trip)

        Spacer(modifier = Modifier.height(8.dp))

        // Action Button Component based on Backend Status
        TripActionButton(
            status = trip.status,
            isActionInProgress = isActionInProgress,
            onStartTrip = onStartTrip,
            onCompleteTrip = {
                val point = gpsState as? DriverGpsState.Active
                val destPoint = GeoPoint(trip.destinationLat, trip.destinationLng)
                val distance = if (point != null) GeoPoint(point.latitude, point.longitude).distanceToAsDouble(destPoint) else 0.0
                if (distance > 100.0) {
                    showDistanceWarning = true
                } else {
                    onCompleteTrip()
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun TripHeaderCard(trip: TripDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
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
                Column {
                    Text(
                        text = "TRIP ID",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = trip.orderNumber,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = when (trip.status.uppercase()) {
                        "TRIP_STARTED", "IN_TRANSIT", "DISPATCHED" -> RouteCJWarning.copy(alpha = 0.2f)
                        "DELIVERED" -> RouteCJSuccess.copy(alpha = 0.2f)
                        else -> RouteCJBlue.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = trip.status.replace("_", " "),
                        color = when (trip.status.uppercase()) {
                            "TRIP_STARTED", "IN_TRANSIT", "DISPATCHED" -> RouteCJWarning
                            "DELIVERED" -> RouteCJSuccess
                            else -> RouteCJCyanLight
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = RouteCJNavyCard, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = RouteCJCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Customer: ${trip.customerName}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun TripTimelineCard(status: String) {
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
            Text(
                text = "TRIP PROGRESS",
                color = RouteCJTextSecondaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            val currentStep = when (status.uppercase()) {
                "ASSIGNED" -> 1
                "DISPATCH_CONFIRMED", "READY_FOR_DISPATCH" -> 2
                "TRIP_STARTED", "DISPATCHED" -> 3
                "IN_TRANSIT" -> 4
                "DELIVERED" -> 5
                else -> 1
            }

            val steps = listOf("Assigned", "Confirmed", "Started", "In Transit", "Delivered")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { index, stepName ->
                    val stepNum = index + 1
                    val isCompleted = stepNum <= currentStep
                    val isCurrent = stepNum == currentStep

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCompleted -> RouteCJCyan
                                        else -> RouteCJNavyCard
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = RouteCJNavyDark,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Text(
                                    text = "$stepNum",
                                    color = RouteCJTextSecondaryDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stepName,
                            color = if (isCurrent) Color.White else RouteCJTextSecondaryDark,
                            fontSize = 9.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TripRouteCard(trip: TripDetails) {
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
            Text(
                text = "ROUTE INFORMATION",
                color = RouteCJTextSecondaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(RouteCJCyan)
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(40.dp)
                            .background(RouteCJNavyCard)
                    )
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(RouteCJSuccess)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Pickup Location",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 11.sp
                    )
                    Text(
                        text = trip.pickupAddress,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Destination",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 11.sp
                    )
                    Text(
                        text = trip.deliveryAddress,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun TripParcelCard(trip: TripDetails) {
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
            Text(
                text = "PARCEL DETAILS",
                color = RouteCJTextSecondaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = trip.itemName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (trip.weight > 0) {
                    Text(
                        text = "Weight: ${trip.weight} kg",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = "Qty: ${trip.quantity}",
                    color = RouteCJTextSecondaryDark,
                    fontSize = 12.sp
                )
                if (trip.isFragile) {
                    Text(
                        text = "• FRAGILE",
                        color = RouteCJWarning,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (trip.specialInstructions.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = RouteCJNavyDark,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Note: ${trip.specialInstructions}",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TripVehicleCard(trip: TripDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = RouteCJCyanLight,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Assigned Vehicle",
                    color = RouteCJTextSecondaryDark,
                    fontSize = 11.sp
                )
                val displayReg = trip.vehicleRegistration?.takeIf { it.isNotBlank() }
                if (displayReg != null) {
                    Text(
                        text = displayReg,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!trip.vehicleType.isNullOrBlank()) {
                        Text(
                            text = "Type: ${trip.vehicleType}",
                            color = RouteCJTextSecondaryDark,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Text(
                        text = "VEHICLE NOT ASSIGNED",
                        color = RouteCJError,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your assigned vehicle could not be found. Contact Fleet Dispatch.",
                        color = RouteCJTextSecondaryDark.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TripActionButton(
    status: String,
    isActionInProgress: Boolean,
    onStartTrip: () -> Unit,
    onCompleteTrip: () -> Unit
) {
    val upperStatus = status.uppercase()

    when (upperStatus) {
        "ASSIGNED", "DISPATCH_CONFIRMED", "READY_FOR_DISPATCH", "PENDING" -> {
            Button(
                onClick = onStartTrip,
                enabled = !isActionInProgress,
                colors = ButtonDefaults.buttonColors(containerColor = RouteCJBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isActionInProgress) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START TRIP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        "TRIP_STARTED", "IN_TRANSIT", "DISPATCHED" -> {
            Button(
                onClick = onCompleteTrip,
                enabled = !isActionInProgress,
                colors = ButtonDefaults.buttonColors(containerColor = RouteCJSuccess),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isActionInProgress) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "COMPLETE DELIVERY",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        "DELIVERED" -> {
            Surface(
                color = RouteCJSuccess.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = RouteCJSuccess
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "DELIVERY COMPLETED",
                        color = RouteCJSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        "CANCELLED", "FAILED" -> {
            Surface(
                color = RouteCJError.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        tint = RouteCJError
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "TRIP CANCELLED",
                        color = RouteCJError,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        else -> {
            Surface(
                color = RouteCJNavyCard,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "STATUS: $status",
                    color = RouteCJTextSecondaryDark,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TripLiveLocationCard(
    tripStatus: String,
    gpsState: DriverGpsState,
    onRequestPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit
) {
    val activeTrip = isTripActive(tripStatus)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when (gpsState) {
                is DriverGpsState.Active -> RouteCJSuccess.copy(alpha = 0.5f)
                is DriverGpsState.Connecting -> RouteCJCyan.copy(alpha = 0.5f)
                is DriverGpsState.LocationDisabled -> RouteCJWarning.copy(alpha = 0.5f)
                is DriverGpsState.PermissionRequired -> RouteCJError.copy(alpha = 0.5f)
                else -> RouteCJCyan.copy(alpha = 0.3f)
            }
        )
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = when (gpsState) {
                            is DriverGpsState.Active -> RouteCJSuccess
                            is DriverGpsState.LocationDisabled -> RouteCJWarning
                            is DriverGpsState.PermissionRequired -> RouteCJError
                            else -> RouteCJCyanLight
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE GPS TRACKING",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Live status chip
                Surface(
                    color = when (gpsState) {
                        is DriverGpsState.Active -> RouteCJSuccess.copy(alpha = 0.18f)
                        is DriverGpsState.LocationDisabled, is DriverGpsState.OfflineWaiting -> RouteCJWarning.copy(alpha = 0.18f)
                        is DriverGpsState.PermissionRequired -> RouteCJError.copy(alpha = 0.18f)
                        else -> RouteCJBlue.copy(alpha = 0.18f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when (gpsState) {
                                        is DriverGpsState.Active -> if (gpsState.isOffline) RouteCJWarning else RouteCJSuccess
                                        is DriverGpsState.Connecting -> RouteCJCyan
                                        is DriverGpsState.LocationDisabled, is DriverGpsState.OfflineWaiting -> RouteCJWarning
                                        is DriverGpsState.PermissionRequired -> RouteCJError
                                        else -> RouteCJCyan
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (gpsState) {
                                is DriverGpsState.Active -> if (gpsState.isOffline) "OFFLINE (LOCAL)" else "SHARING ACTIVE"
                                is DriverGpsState.Connecting -> "CONNECTING"
                                is DriverGpsState.LocationDisabled -> "LOCATION OFF"
                                is DriverGpsState.PermissionRequired -> "PERMISSION NEEDED"
                                is DriverGpsState.OfflineWaiting -> "OFFLINE"
                                is DriverGpsState.WaitingForSignal -> "WAITING FOR GPS SIGNAL"
                                is DriverGpsState.StartFailed -> "ERROR"
                                DriverGpsState.Inactive -> if (activeTrip) "CONNECTING" else "INACTIVE"
                            },
                            color = when (gpsState) {
                                is DriverGpsState.Active -> if (gpsState.isOffline) RouteCJWarning else RouteCJSuccess
                                is DriverGpsState.Connecting -> RouteCJCyanLight
                                is DriverGpsState.LocationDisabled, is DriverGpsState.OfflineWaiting -> RouteCJWarning
                                is DriverGpsState.PermissionRequired -> RouteCJError
                                else -> RouteCJCyanLight
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (gpsState) {
                is DriverGpsState.Active -> {
                    val timeFormat = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Last Updated",
                                color = RouteCJTextSecondaryDark,
                                fontSize = 11.sp
                            )
                            Text(
                                text = timeFormat.format(gpsState.timestamp),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Column {
                            Text(
                                text = "GPS Accuracy",
                                color = RouteCJTextSecondaryDark,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "±${gpsState.accuracy.toInt()} m",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Column {
                            Text(
                                text = "Speed",
                                color = RouteCJTextSecondaryDark,
                                fontSize = 11.sp
                            )
                            val speedKmh = (gpsState.speed * 3.6f).toInt()
                            Text(
                                text = "$speedKmh km/h",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (gpsState.isLastKnownLocation) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Using last known location — waiting for fresh GPS",
                            color = RouteCJWarning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                is DriverGpsState.Connecting -> {
                    Text(
                        text = "Initializing GPS telemetry service...",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 12.sp
                    )
                }

                is DriverGpsState.LocationDisabled -> {
                    Column {
                        Text(
                            text = "Device Location/GPS service is turned OFF. Turn on Location to continue live delivery tracking.",
                            color = RouteCJTextSecondaryDark,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onOpenLocationSettings,
                            colors = ButtonDefaults.buttonColors(containerColor = RouteCJWarning),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOff,
                                contentDescription = null,
                                tint = RouteCJNavyDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TURN ON LOCATION",
                                color = RouteCJNavyDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                is DriverGpsState.StartFailed -> {
                    Column {
                        Text(
                            text = "GPS COULD NOT START",
                            color = RouteCJError,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Please enable Location and allow RouteCJ Driver to use location.",
                            color = RouteCJTextSecondaryDark,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onOpenLocationSettings,
                            colors = ButtonDefaults.buttonColors(containerColor = RouteCJWarning),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOff,
                                contentDescription = null,
                                tint = RouteCJNavyDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "OPEN LOCATION SETTINGS",
                                color = RouteCJNavyDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                is DriverGpsState.PermissionRequired -> {
                    Column {
                        Text(
                            text = "RouteCJ needs location permission to broadcast live GPS telemetry during active deliveries.",
                            color = RouteCJTextSecondaryDark,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = RouteCJBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ALLOW LOCATION PERMISSION",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                is DriverGpsState.OfflineWaiting -> {
                    Text(
                        text = "Network offline: GPS telemetry will synchronize to RouteCJ as soon as connectivity is restored.",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 12.sp
                    )
                }

                is DriverGpsState.WaitingForSignal -> {
                    Text(
                        text = "Acquiring GPS satellite fix...",
                        color = RouteCJCyanLight,
                        fontSize = 12.sp
                    )
                }

                DriverGpsState.Inactive -> {
                    Text(
                        text = "GPS telemetry is active only during started/in-transit trips.",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
