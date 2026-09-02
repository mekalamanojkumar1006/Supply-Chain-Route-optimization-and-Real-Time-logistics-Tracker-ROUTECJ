package com.routecj.admin.presentation.tracking

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.DispatchStatus
import com.routecj.admin.domain.model.TrackingInfo
import com.routecj.admin.presentation.components.*
import com.routecj.admin.presentation.tracking.components.OsmTrackingMapView
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    navController: NavController,
    viewModel: TrackingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val filteredTripsState by viewModel.filteredTripsState.collectAsStateWithLifecycle()
    val rawTripsState by viewModel.rawTripsState.collectAsStateWithLifecycle()
    val selectedTrip by viewModel.selectedTrip.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val mainStore by viewModel.mainStore.collectAsStateWithLifecycle()

    LaunchedEffect(actionState) {
        if (actionState is Result.Success) {
            Toast.makeText(context, "Action completed successfully!", Toast.LENGTH_SHORT).show()
            viewModel.clearActionState()
        } else if (actionState is Result.Error) {
            Toast.makeText(context, (actionState as Result.Error).message, Toast.LENGTH_LONG).show()
            viewModel.clearActionState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = if (selectedTrip == null) "Active Trips Command Center" else "Active Trip Details",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        if (selectedTrip == null && rawTripsState is Result.Success) {
                            val activeCount = (rawTripsState as Result.Success).data.count { it.status == DispatchStatus.IN_TRANSIT || it.status == DispatchStatus.TRIP_STARTED }
                            Text(
                                text = "$activeCount active units in transit",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (selectedTrip == null) navController.popBackStack()
                        else viewModel.selectTrip(null)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (selectedTrip != null) {
                TripTrackingHeroView(
                    trip = selectedTrip!!,
                    mainStore = mainStore,
                    onBack = { viewModel.selectTrip(null) },
                    onUpdateStatus = { status -> viewModel.updateDispatchStatus(selectedTrip!!.dispatchId, status) }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search Bar
                    PremiumSearchBar(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = "Search trip, order, driver, vehicle, or item..."
                    )

                    // Status Filter Tabs
                    ScrollableTabRow(
                        selectedTabIndex = TripFilterTab.entries.indexOf(selectedTab),
                        edgePadding = 16.dp,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = {}
                    ) {
                        TripFilterTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onTabSelected(tab) },
                                label = { Text(tab.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.LightGray
                                ),
                                border = null,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // Content based on state
                    when (val state = filteredTripsState) {
                        is Result.Loading -> PremiumLoadingState(message = "Tracking active units across OpenStreetMap...")
                        is Result.Success -> {
                            if (state.data.isEmpty()) {
                                PremiumEmptyState(
                                    message = if (searchQuery.isNotBlank()) "No trips match '$searchQuery'" else "No active trips in this category.",
                                    icon = Icons.Default.LocalShipping
                                )
                            } else {
                                ActiveTripsSpatialList(trips = state.data) { viewModel.selectTrip(it) }
                            }
                        }
                        is Result.Error -> PremiumErrorState(message = state.message, onRetry = { })
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveTripsSpatialList(trips: List<TrackingInfo>, onSelect: (TrackingInfo) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp), 
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(trips) { trip ->
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSelect(trip) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TRIP #${trip.orderNumber}",
                            fontWeight = FontWeight.ExtraBold,
                            color = Primary,
                            letterSpacing = 1.sp,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = trip.itemName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                    PremiumStatusChip(
                        text = trip.status.name.replace("_", " "),
                        color = when (trip.status) {
                            DispatchStatus.TRIP_STARTED, DispatchStatus.IN_TRANSIT -> Primary
                            DispatchStatus.DELIVERED -> Color(0xFF10B981)
                            DispatchStatus.CANCELLED -> Color(0xFFEF4444)
                            else -> Color(0xFFF59E0B)
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Route
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.TripOrigin, contentDescription = null, modifier = Modifier.size(14.dp), tint = Primary)
                    Text(text = trip.pickupLocation, fontSize = 12.sp, color = Color.LightGray, maxLines = 1, fontWeight = FontWeight.Medium)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFEF4444))
                    Text(text = trip.deliveryLocation, fontSize = 12.sp, color = Color.LightGray, maxLines = 1, fontWeight = FontWeight.Medium)
                }
                
                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { trip.progressPercentage / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Primary,
                    trackColor = Color(0xFF334155),
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF334155))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(text = "Driver: ${trip.driverName}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Secondary)
                        Text(text = "Vehicle: ${trip.vehicleRegistration} (${trip.vehicleType})", fontSize = 11.sp, color = Color.Gray)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val connectionColor = if (trip.isLocationStale) Color(0xFFEF4444) else Color(0xFF22C55E)
                        Box(modifier = Modifier.size(8.dp).background(connectionColor, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (trip.isLocationStale) "STALE" else "LIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = connectionColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TripTrackingHeroView(
    trip: TrackingInfo,
    mainStore: com.routecj.admin.domain.model.Godown?,
    onBack: () -> Unit,
    onUpdateStatus: (DispatchStatus) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // OpenStreetMap Component (Zero API Cost, No Google Maps API Key)
        OsmTrackingMapView(
            trip = trip,
            storeLocation = mainStore,
            modifier = Modifier.fillMaxSize()
        )

        // Floating Spatial Surface Control Card
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SpatialSurface(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            val tripDirectionText = when (trip.status) {
                                DispatchStatus.ASSIGNED, DispatchStatus.DISPATCH_CONFIRMED -> "GOING TO PICKUP"
                                DispatchStatus.TRIP_STARTED, DispatchStatus.IN_TRANSIT -> "LEAVING STORE"
                                DispatchStatus.DELIVERED -> "TRIP COMPLETED"
                                else -> "ACTIVE TRIP"
                            }
                            Text(text = tripDirectionText, style = MaterialTheme.typography.labelSmall, color = Primary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Text(text = "Order #${trip.orderNumber}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Secondary)
                            Text(text = trip.itemName, fontSize = 13.sp, color = Color.LightGray, fontWeight = FontWeight.Medium)
                        }
                        PremiumStatusChip(
                            text = trip.status.name.replace("_", " "),
                            color = when (trip.status) {
                                DispatchStatus.TRIP_STARTED, DispatchStatus.IN_TRANSIT -> Primary
                                DispatchStatus.DELIVERED -> Color(0xFF10B981)
                                else -> Color(0xFFF59E0B)
                            }
                        )
                    }

                    // Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Trip Progress", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("${trip.progressPercentage}%", fontSize = 12.sp, color = Primary, fontWeight = FontWeight.Black)
                        }
                        LinearProgressIndicator(
                            progress = { trip.progressPercentage / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = Primary,
                            trackColor = Color(0xFF334155),
                        )
                    }

                    // Route details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(Primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(text = "ROUTE", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                            Text(text = "${trip.pickupLocation} → ${trip.deliveryLocation}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Secondary)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF334155))

                    // Personnel & Vehicle Information
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (trip.driverId.isNotBlank()) {
                                    onBack()
                                    // Navigate to full Driver Location screen
                                    // (handled via caller or direct nav)
                                }
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Driver: ${trip.driverName}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Secondary)
                            if (trip.driverPhone.isNotBlank()) {
                                Text(text = "Phone: ${trip.driverPhone}", fontSize = 12.sp, color = Color.LightGray)
                            }
                            Text(text = "Vehicle: ${trip.vehicleRegistration} (${trip.vehicleType})", fontSize = 12.sp, color = Color.Gray)
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            if (trip.isLocationStale || trip.currentLatitude == null || trip.currentLatitude == 0.0) {
                                Surface(color = Color(0xFFEF4444).copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(6.dp).background(Color(0xFFEF4444), CircleShape))
                                        Text(text = "LOCATION OFFLINE", color = Color(0xFFFCA5A5), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            } else {
                                UnitPulseIndicator()
                            }

                            if (trip.speed > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "${trip.speed.toInt()} km/h", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Primary)
                            }
                        }
                    }

                    // Delivery Status / Lifecycle Info Card
                    if (trip.status == DispatchStatus.DELIVERED) {
                        HorizontalDivider(color = Color(0xFF334155))
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Delivered & Verified via Driver OTP",
                                    color = Color(0xFF10B981),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (trip.status == DispatchStatus.IN_TRANSIT || trip.status == DispatchStatus.TRIP_STARTED) {
                        HorizontalDivider(color = Color(0xFF334155))
                        Surface(
                            color = Primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "In Transit • Waiting for driver delivery verification...",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (trip.status == DispatchStatus.DISPATCH_CONFIRMED || trip.status == DispatchStatus.ASSIGNED) {
                        HorizontalDivider(color = Color(0xFF334155))
                        Button(
                            onClick = { onUpdateStatus(DispatchStatus.TRIP_STARTED) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("Start Trip", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnitPulseIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse_alpha"
    )
    
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(8.dp).background(Color(0xFF22C55E).copy(alpha = alpha), CircleShape))
        Text(text = "LIVE GPS", color = Color(0xFF22C55E), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
    }
}
