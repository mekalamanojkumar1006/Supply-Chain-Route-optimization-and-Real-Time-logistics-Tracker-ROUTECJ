package com.routecj.admin.presentation.tracking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.routecj.admin.domain.model.Driver
import com.routecj.admin.domain.model.Vehicle
import com.routecj.admin.presentation.components.BentoCard
import com.routecj.admin.presentation.components.PremiumLoadingState
import com.routecj.admin.presentation.components.PremiumStatusChip
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverLocationScreen(
    navController: NavController,
    driverId: String,
    viewModel: DriverLocationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val followDriver by viewModel.followDriver.collectAsStateWithLifecycle()

    var showMarkerDetailsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(driverId) {
        viewModel.observeDriverLocation(driverId)
    }

    val driver = uiState.driver
    val driverLat = driver?.currentLatitude
    val driverLng = driver?.currentLongitude
    val hasGps = driverLat != null && driverLng != null && driverLat != 0.0 && driverLng != 0.0

    // MapView instance retained across recompositions
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 4.0
            maxZoomLevel = 20.0
            controller.setZoom(16.5)
        }
    }

    // Lifecycle management for osmdroid
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDetach()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Custom Driver Marker Icon with heading rotation
    val driverMarkerIcon = remember { createDriverLocationMarkerDrawable(context) }

    val driverMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = driverMarkerIcon
            setOnMarkerClickListener { _, _ ->
                showMarkerDetailsSheet = true
                true
            }
        }
    }

    // Realtime Marker updates
    LaunchedEffect(driverLat, driverLng, driver?.heading, driver?.speed, followDriver) {
        if (hasGps && driver != null) {
            val point = GeoPoint(driverLat!!, driverLng!!)
            driverMarker.position = point
            driverMarker.rotation = driver.heading.toFloat()
            driverMarker.title = driver.name
            driverMarker.snippet = "Speed: ${driver.speed.toInt()} km/h • Accuracy: ±${driver.accuracy.toInt()}m"

            if (!mapView.overlays.contains(driverMarker)) {
                mapView.overlays.add(driverMarker)
            }

            if (followDriver) {
                mapView.controller.animateTo(point)
            }
            mapView.invalidate()
        } else {
            mapView.overlays.remove(driverMarker)
            mapView.invalidate()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = driver?.name?.ifBlank { "Driver Live Location" } ?: "Driver Live Location",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = Secondary
                        )
                        Text(
                            text = if (driver != null) "ID: ${driver.id.take(8).uppercase()}" else "Resolving...",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Follow Driver Toggle Chip
                    FilterChip(
                        selected = followDriver,
                        onClick = { viewModel.toggleFollowDriver() },
                        label = {
                            Text(
                                text = if (followDriver) "Following" else "Free View",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (followDriver) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (driver == null) {
                PremiumLoadingState(message = "Connecting to driver's GPS telemetry...")
            } else {
                // 1. AndroidView holding the MapView
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize()
                )

                // 2. Driver Details Top Float Header
                DriverLiveDetailsHeader(
                    driver = driver,
                    vehicle = uiState.assignedVehicle,
                    isLive = uiState.isLive,
                    isStale = uiState.isStale,
                    isUnavailable = uiState.isUnavailable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )

                // 3. Floating Map Camera Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Recenter on Driver Button
                    FloatingControlAction(
                        icon = Icons.Default.MyLocation,
                        contentDescription = "Center on Driver",
                        onClick = {
                            if (hasGps) {
                                viewModel.setFollowDriver(true)
                                mapView.controller.animateTo(GeoPoint(driverLat!!, driverLng!!))
                            }
                        }
                    )

                    // Zoom In Button
                    FloatingControlAction(
                        icon = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        onClick = { mapView.controller.zoomIn() }
                    )

                    // Zoom Out Button
                    FloatingControlAction(
                        icon = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        onClick = { mapView.controller.zoomOut() }
                    )
                }

                // 4. Quick Details Bottom Floating Bar
                DriverBottomTelemetryBar(
                    driver = driver,
                    lastUpdatedFormatted = uiState.lastUpdatedFormatted,
                    onInspectClick = { showMarkerDetailsSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                )

                // 5. OSM Required Attribution
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 14.dp, bottom = 120.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = "© OpenStreetMap contributors",
                        fontSize = 9.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }

    // Comprehensive Driver Marker Details Dialog / Modal
    if (showMarkerDetailsSheet && driver != null) {
        DriverTelemetryModal(
            driver = driver,
            vehicle = uiState.assignedVehicle,
            lastUpdated = uiState.lastUpdatedFormatted,
            isLive = uiState.isLive,
            onDismiss = { showMarkerDetailsSheet = false }
        )
    }
}

@Composable
fun DriverLiveDetailsHeader(
    driver: Driver,
    vehicle: Vehicle?,
    isLive: Boolean,
    isStale: Boolean,
    isUnavailable: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(20.dp)),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = driver.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Secondary
                    )

                    // Live Status Chip
                    val (statusText, statusColor) = when {
                        isLive -> "● LIVE" to Color(0xFF22C55E)
                        isStale -> "● STALE GPS" to Color(0xFFF59E0B)
                        else -> "● NO GPS" to Color(0xFFEF4444)
                    }

                    PremiumStatusChip(text = statusText, color = statusColor)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vehicle: ${vehicle?.vehicleNumber ?: vehicle?.registrationNumber ?: driver.assignedVehicle ?: "Unassigned"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )

                    Text(
                        text = "Speed: ${driver.speed.toInt()} km/h",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Primary
                    )
                }
            }
        }
    }
}

@Composable
fun DriverBottomTelemetryBar(
    driver: Driver,
    lastUpdatedFormatted: String,
    onInspectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .clickable { onInspectClick() },
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "TELEMETRY METRICS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Primary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Updated: $lastUpdatedFormatted",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.LightGray
                )
            }

            Button(
                onClick = onInspectClick,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Inspect", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun DriverTelemetryModal(
    driver: Driver,
    vehicle: Vehicle?,
    lastUpdated: String,
    isLive: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.GpsFixed, contentDescription = null, tint = Primary)
                Text("Driver Telemetry", fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryRowItem(Icons.Default.Person, "DRIVER NAME", driver.name)
                TelemetryRowItem(Icons.Default.Badge, "DRIVER ID", driver.id)
                TelemetryRowItem(Icons.Default.Phone, "CONTACT NUMBER", driver.phone.ifBlank { "Not provided" })
                TelemetryRowItem(Icons.Default.LocalShipping, "ASSIGNED VEHICLE", vehicle?.vehicleNumber ?: vehicle?.registrationNumber ?: driver.assignedVehicle ?: "Unassigned")
                TelemetryRowItem(Icons.Default.Speed, "CURRENT SPEED", "${driver.speed.toInt()} km/h")
                TelemetryRowItem(Icons.Default.Navigation, "BEARING / HEADING", "${driver.heading.toInt()}°")
                TelemetryRowItem(Icons.Default.Adjust, "GPS ACCURACY", "±${driver.accuracy.toInt()} meters")
                TelemetryRowItem(Icons.Default.PinDrop, "GPS COORDINATES", "${driver.currentLatitude.format(5)}, ${driver.currentLongitude.format(5)}")
                TelemetryRowItem(Icons.Default.Update, "LAST SYNCED", lastUpdated)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold, color = Primary)
            }
        }
    )
}

@Composable
fun TelemetryRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier.size(30.dp).background(Primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Secondary)
        }
    }
}

@Composable
private fun FloatingControlAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 6.dp,
        modifier = Modifier.size(44.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Secondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun createDriverLocationMarkerDrawable(context: Context): BitmapDrawable {
    val drawable = androidx.core.content.ContextCompat.getDrawable(
        context,
        com.routecj.admin.R.drawable.ic_routecj_truck_marker
    ) ?: return createFallbackTruckMarkerDrawable(context)

    val sizePx = (54 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    drawable.setBounds(0, 0, sizePx, sizePx)
    drawable.draw(canvas)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createFallbackTruckMarkerDrawable(context: Context): BitmapDrawable {
    val sizePx = (52 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val center = sizePx / 2f
    val outerRadius = sizePx * 0.44f
    val innerRadius = sizePx * 0.32f
    val coreRadius = sizePx * 0.16f

    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(80, 0, 207, 200)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, outerRadius, haloPaint)

    val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(0, 207, 200)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, innerRadius, primaryPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3 * context.resources.displayMetrics.density
    }
    canvas.drawCircle(center, center, innerRadius, strokePaint)

    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(15, 23, 42)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, coreRadius, corePaint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun Double.format(digits: Int): String = "%.${digits}f".format(this)
