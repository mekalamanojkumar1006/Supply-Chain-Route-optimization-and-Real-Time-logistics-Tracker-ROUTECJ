package com.routecj.customer.presentation.booking

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.routecj.customer.presentation.components.*
import com.routecj.customer.ui.theme.RouteCJSpacing
import com.routecj.customer.ui.theme.StitchTonalBorder

@Composable
fun PickupLocationScreen(
    viewModel: BookingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateNext: () -> Unit
) {
    val locationState by viewModel.locationState.collectAsState()
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (isGranted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    LaunchedEffect(locationState) {
        if (locationState is LocationState.RequestingPermission) {
            val hasFineLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasFineLocation) {
                viewModel.onPermissionGranted()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            RouteCJTopBar(
                title = "Create Delivery",
                onBackClick = onNavigateBack
            )
        },
        bottomBar = {
            if (locationState is LocationState.Success) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StitchTonalBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding()
                    ) {
                        RouteCJButton(
                            text = "Continue to Destination",
                            onClick = onNavigateNext
                        )
                    }
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stitch Step Progress Header
            RouteCJStepProgressHeader(
                currentStep = 1,
                totalSteps = 4,
                stepTitle = "Pickup Location"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hero Question
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Where should we collect the package?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Confirm your current location or drop a pin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (val state = locationState) {
                is LocationState.Idle -> {
                    RouteCJCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(RouteCJSpacing.Large),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Locating Pickup Point",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "RouteCJ uses your GPS location to set the pickup point for your shipment.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            RouteCJButton(
                                text = "Use Current Location",
                                onClick = { viewModel.requestLocation() },
                                icon = Icons.Default.MyLocation
                            )
                        }
                    }
                }
                is LocationState.RequestingPermission -> {
                    // Handled by LaunchedEffect
                }
                is LocationState.Loading -> {
                    RouteCJLoading(modifier = Modifier.padding(vertical = 32.dp))
                }
                is LocationState.Error -> {
                    RouteCJErrorState(
                        message = state.message,
                        onRetry = { viewModel.requestLocation() }
                    )
                }
                is LocationState.Success -> {
                    // Stitch Location Card
                    RouteCJCard(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = state.address?.split(",")?.firstOrNull()?.trim() ?: "Pickup Location",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = state.address ?: "Lat: ${"%.4f".format(state.latitude)}, Lng: ${"%.4f".format(state.longitude)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.requestLocation() },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh Location",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Meta Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(percent = 50),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Est. pickup: 10 mins",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(percent = 50),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalShipping,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "5 drivers near",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Map Section Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Adjust Map",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Drag or tap to adjust pin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // OpenStreetMap osmdroid Component
                    RouteCJMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        pickupLocation = Pair(state.latitude, state.longitude),
                        showDriver = false,
                        showRoute = false,
                        zoomLevel = 15.0,
                        enableZoomControls = true,
                        onPickupLocationChanged = { lat, lng ->
                            viewModel.updatePickupLocation(lat, lng)
                        },
                        onMapClick = { point ->
                            viewModel.updatePickupLocation(point.first, point.second)
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

