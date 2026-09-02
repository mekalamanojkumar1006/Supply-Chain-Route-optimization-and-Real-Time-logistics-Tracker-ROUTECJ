package com.routecj.customer.presentation.tracking

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.routecj.customer.domain.model.DriverLocation
import com.routecj.customer.domain.model.Order
import com.routecj.customer.domain.model.OrderStatus
import com.routecj.customer.presentation.components.RouteCJAvatar
import com.routecj.customer.presentation.components.RouteCJErrorState
import com.routecj.customer.presentation.components.RouteCJLoading
import com.routecj.customer.presentation.components.RouteCJMap
import com.routecj.customer.presentation.components.RouteCJStatusBadge
import com.routecj.customer.presentation.components.RouteCJTopBar
import com.routecj.customer.ui.theme.BrandPrimaryBlue
import com.routecj.customer.ui.theme.RouteCJSpacing
import com.routecj.customer.ui.theme.StitchTonalBorder
import com.routecj.customer.ui.theme.TertiaryDark

@Composable
fun TrackingScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrackingViewModel = hiltViewModel()
) {
    val trackingState by viewModel.trackingState.collectAsState()
    val orderState by viewModel.orderState.collectAsState()
    val routePoints by viewModel.routePoints.collectAsState()
    val routeError by viewModel.routeError.collectAsState()

    Scaffold(
        topBar = {
            RouteCJTopBar(
                title = "Live Delivery Tracking",
                onBackClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = trackingState) {
                is TrackingState.Loading -> {
                    RouteCJLoading(modifier = Modifier.align(Alignment.Center))
                }
                is TrackingState.NoLocationYet -> {
                    TrackingWithMap(
                        driverLocation = null,
                        order = orderState,
                        routePoints = routePoints,
                        routeError = routeError,
                        statusMessage = "Awaiting driver location update..."
                    )
                }
                is TrackingState.Unavailable -> {
                    if (orderState != null) {
                        TrackingWithMap(
                            driverLocation = null,
                            order = orderState,
                            routePoints = routePoints,
                            routeError = routeError,
                            statusMessage = state.reason
                        )
                    } else {
                        RouteCJErrorState(
                            message = state.reason,
                            modifier = Modifier.padding(16.dp).align(Alignment.Center)
                        )
                    }
                }
                is TrackingState.Active -> {
                    TrackingWithMap(
                        driverLocation = state.location,
                        order = orderState,
                        routePoints = routePoints,
                        routeError = routeError,
                        isStale = false
                    )
                }
                is TrackingState.Stale -> {
                    TrackingWithMap(
                        driverLocation = state.location,
                        order = orderState,
                        routePoints = routePoints,
                        routeError = routeError,
                        isStale = true
                    )
                }
            }
        }
    }
}

@Composable
fun TrackingWithMap(
    driverLocation: DriverLocation?,
    order: Order?,
    routePoints: List<Pair<Double, Double>>,
    routeError: String?,
    isStale: Boolean = false,
    statusMessage: String? = null
) {
    val context = LocalContext.current
    val pickupPair = order?.let { Pair(it.pickupLatitude, it.pickupLongitude) }
    val destPair = order?.let {
        if (it.destinationLatitude != null && it.destinationLongitude != null) {
            Pair(it.destinationLatitude, it.destinationLongitude)
        } else null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Full OpenStreetMap View (osmdroid)
        RouteCJMap(
            modifier = Modifier.fillMaxSize(),
            pickupLocation = pickupPair,
            destinationLocation = destPair,
            driverLocation = driverLocation,
            routePoints = routePoints,
            showDriver = driverLocation != null,
            showRoute = true,
            routeError = routeError,
            enableZoomControls = true
        )

        // 2. Floating Top ETA Pill Card
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, StitchTonalBorder),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_eta")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "eta_alpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .alpha(alpha)
                            .clip(CircleShape)
                            .background(TertiaryDark)
                    )
                    Text(
                        text = if (isStale) "GPS Reconnecting" else "ETA: 14 mins",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "3.2 km remaining",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 3. Floating Bottom Driver & Status Bottom Sheet Card
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, StitchTonalBorder),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Driver Profile & Call Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RouteCJAvatar(name = "Driver", size = 48.dp)

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Marcus Vance",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(percent = 50),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB800),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "4.9",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Freightliner Cascadia • KA-01-EQ-9832",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Call button
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919876543210"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(BrandPrimaryBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call Driver",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // Delivery Status Steps Timeline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrackingTimelineStep(
                        label = "Picked Up",
                        isCompleted = true,
                        isActive = false
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(TertiaryDark)
                    )
                    TrackingTimelineStep(
                        label = "In Transit",
                        isCompleted = false,
                        isActive = true
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    )
                    TrackingTimelineStep(
                        label = "Delivered",
                        isCompleted = false,
                        isActive = false
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackingTimelineStep(
    label: String,
    isCompleted: Boolean,
    isActive: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> TertiaryDark
                        isActive -> BrandPrimaryBlue
                        else -> MaterialTheme.colorScheme.surfaceContainerHighest
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
            } else if (isActive) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive || isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

