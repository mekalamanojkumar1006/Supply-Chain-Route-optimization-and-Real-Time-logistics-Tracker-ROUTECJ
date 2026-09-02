package com.routecj.driver.presentation.map

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.routecj.driver.core.routing.OsrmRouteResult
import com.routecj.driver.domain.model.StoreLocation
import com.routecj.driver.domain.model.TripDetails
import com.routecj.driver.presentation.components.ErrorState
import com.routecj.driver.presentation.components.LoadingState
import com.routecj.driver.service.DriverGpsState
import com.routecj.driver.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverMapScreen(
    tripId: String,
    driverId: String,
    driverMapViewModel: DriverMapViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by driverMapViewModel.uiState.collectAsState()

    LaunchedEffect(tripId, driverId) {
        Configuration.getInstance().userAgentValue = context.packageName
        driverMapViewModel.initialize(tripId, driverId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Live Navigation",
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
                is DriverMapUiState.Loading -> {
                    LoadingState(message = "Loading Map & Route...", modifier = Modifier.fillMaxSize())
                }

                is DriverMapUiState.Error -> {
                    ErrorState(
                        message = "UNABLE TO LOAD MAP\n${state.message}",
                        actionText = "RETRY",
                        onAction = { driverMapViewModel.retry() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is DriverMapUiState.Active -> {
                    ActiveMapLayout(
                        state = state,
                        onOpenLocationSettings = {
                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            context.startActivity(intent)
                        },
                        onNavigateBackToTrip = onBack
                    )
                }
            }
        }
    }
}

/**
 * Creates a custom circular badge pin bitmap with high contrast and explicit pixel dimensions.
 * Prevents Osmdroid from ever falling back to its internal red pin.
 */
fun createMarkerBadgeBitmap(
    context: Context,
    vectorResId: Int,
    badgeColorHex: String,
    iconTintColorHex: String = "#FFFFFF",
    badgeSizeDp: Int = 48
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (badgeSizeDp * density).toInt()

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor(badgeColorHex)
        style = Paint.Style.FILL
    }
    val radius = sizePx / 2f
    canvas.drawCircle(radius, radius, radius - (2 * density), bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
    }
    canvas.drawCircle(radius, radius, radius - (2 * density), borderPaint)

    val drawable = ContextCompat.getDrawable(context, vectorResId)
    if (drawable != null) {
        val iconSizePx = (sizePx * 0.55f).toInt()
        val offset = (sizePx - iconSizePx) / 2
        drawable.setBounds(offset, offset, offset + iconSizePx, offset + iconSizePx)
        drawable.setTint(AndroidColor.parseColor(iconTintColorHex))
        drawable.draw(canvas)
    }

    return BitmapDrawable(context.resources, bitmap)
}

fun createStoreMarker(
    mapView: MapView,
    context: Context,
    position: GeoPoint,
    title: String,
    snippet: String
): Marker {
    val storeMarker = Marker(mapView)
    storeMarker.position = position
    storeMarker.title = title
    storeMarker.snippet = snippet
    storeMarker.icon = createMarkerBadgeBitmap(
        context = context,
        vectorResId = com.routecj.driver.R.drawable.ic_store_marker,
        badgeColorHex = "#0B172A", // RouteCJ Navy
        iconTintColorHex = "#00C7C7", // RouteCJ Cyan
        badgeSizeDp = 48
    )
    storeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    return storeMarker
}

fun createDriverMarker(
    mapView: MapView,
    context: Context,
    position: GeoPoint,
    title: String,
    snippet: String,
    bearing: Float
): Marker {
    val driverMarker = Marker(mapView)
    driverMarker.position = position
    driverMarker.title = title
    driverMarker.snippet = snippet
    driverMarker.icon = createMarkerBadgeBitmap(
        context = context,
        vectorResId = com.routecj.driver.R.drawable.ic_white_truck,
        badgeColorHex = "#00C7C7", // RouteCJ Cyan
        iconTintColorHex = "#FFFFFF",
        badgeSizeDp = 44
    )
    driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    driverMarker.rotation = -bearing
    return driverMarker
}

fun createPickupMarker(
    mapView: MapView,
    context: Context,
    position: GeoPoint,
    title: String,
    snippet: String
): Marker {
    val pickupMarker = Marker(mapView)
    pickupMarker.position = position
    pickupMarker.title = title
    pickupMarker.snippet = snippet
    pickupMarker.icon = createMarkerBadgeBitmap(
        context = context,
        vectorResId = android.R.drawable.ic_menu_myplaces,
        badgeColorHex = "#10B981", // RouteCJ Success Green
        iconTintColorHex = "#FFFFFF",
        badgeSizeDp = 48
    )
    pickupMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    return pickupMarker
}

fun createDestinationMarker(
    mapView: MapView,
    context: Context,
    position: GeoPoint,
    title: String,
    snippet: String
): Marker {
    val destMarker = Marker(mapView)
    destMarker.position = position
    destMarker.title = title
    destMarker.snippet = snippet
    destMarker.icon = createMarkerBadgeBitmap(
        context = context,
        vectorResId = android.R.drawable.ic_menu_directions,
        badgeColorHex = "#3B82F6", // RouteCJ Delivery Blue
        iconTintColorHex = "#FFFFFF",
        badgeSizeDp = 48
    )
    destMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    return destMarker
}

@Composable
fun ActiveMapLayout(
    state: DriverMapUiState.Active,
    onOpenLocationSettings: () -> Unit,
    onNavigateBackToTrip: () -> Unit
) {
    val context = LocalContext.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var isFirstLocationFix by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose {
            mapViewRef?.onDetach()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(16.0)

                    val initialCenter = state.targetPoint
                        ?: state.driverLocation
                        ?: state.selectedStore?.let { GeoPoint(it.latitude, it.longitude) }
                        ?: GeoPoint(18.1085, 83.3988)
                    controller.setCenter(initialCenter)
                    mapViewRef = this
                }
            },
            update = { map ->
                map.overlays.clear()

                // 1. Store Marker (RouteCJ Cyan Store Icon)
                if (state.selectedStore != null) {
                    val storeGeoPoint = GeoPoint(state.selectedStore.latitude, state.selectedStore.longitude)
                    val storeMarker = createStoreMarker(
                        mapView = map,
                        context = context,
                        position = storeGeoPoint,
                        title = state.selectedStore.name,
                        snippet = state.selectedStore.address
                    )
                    map.overlays.add(storeMarker)
                }

                // 2. Driver Marker (RouteCJ White Truck)
                if (state.driverLocation != null) {
                    val gpsActive = state.gpsState as? DriverGpsState.Active
                    val snippetText = if (gpsActive?.isLastKnownLocation == true) "Last Known Location" else "Live Vehicle Location"
                    val bearing = gpsActive?.bearing ?: 0f

                    val driverMarker = createDriverMarker(
                        mapView = map,
                        context = context,
                        position = state.driverLocation,
                        title = "RouteCJ Driver",
                        snippet = snippetText,
                        bearing = bearing
                    )
                    map.overlays.add(driverMarker)

                    if (isFirstLocationFix) {
                        map.controller.animateTo(state.driverLocation)
                        isFirstLocationFix = false
                    }
                } else if (isFirstLocationFix && state.targetPoint != null) {
                    map.controller.setCenter(state.targetPoint)
                    isFirstLocationFix = false
                }

                // 3. Customer Pickup Marker
                if (state.pickupLocation != null && state.tripDetails != null) {
                    val pickupMarker = createPickupMarker(
                        mapView = map,
                        context = context,
                        position = state.pickupLocation,
                        title = "CUSTOMER PICKUP",
                        snippet = state.tripDetails.pickupAddress
                    )
                    map.overlays.add(pickupMarker)
                }

                // 4. Customer Delivery Marker
                if (state.destinationLocation != null && state.tripDetails != null) {
                    val destMarker = createDestinationMarker(
                        mapView = map,
                        context = context,
                        position = state.destinationLocation,
                        title = "CUSTOMER DELIVERY",
                        snippet = state.tripDetails.deliveryAddress
                    )
                    map.overlays.add(destMarker)
                }

                // 5. Polyline Route from OSRM
                if (state.routeResult != null && state.routeResult.points.isNotEmpty()) {
                    val polyline = Polyline().apply {
                        setPoints(state.routeResult.points)
                        outlinePaint.color = AndroidColor.parseColor("#00C7C7") // RouteCJ Cyan
                        outlinePaint.strokeWidth = 12f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        outlinePaint.strokeJoin = Paint.Join.ROUND
                    }
                    map.overlays.add(polyline)
                }

                map.invalidate()
            }
        )

        // Overlay 1: Top Header (Trip Info or Store Info)
        if (state.tripDetails != null) {
            TopTripInfoCard(
                tripDetails = state.tripDetails,
                routeResult = state.routeResult,
                targetLabel = state.targetLabel,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        } else if (state.selectedStore != null) {
            TopStoreInfoCard(
                storeLocation = state.selectedStore,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        }

        // Overlay 2: GPS Status Warning Banner
        if (state.gpsState is DriverGpsState.LocationDisabled || state.gpsState is DriverGpsState.PermissionRequired || state.gpsState is DriverGpsState.OfflineWaiting || state.gpsState is DriverGpsState.Connecting || state.gpsState is DriverGpsState.WaitingForSignal || state.isRoutingFailed) {
            LocationWarningBanner(
                gpsState = state.gpsState,
                isRoutingFailed = state.isRoutingFailed,
                onOpenSettings = onOpenLocationSettings,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 110.dp, start = 16.dp, end = 16.dp)
            )
        }

        // Floating Action Buttons Column (Store Location, My Location, Target Location)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 140.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // STORE LOCATION BUTTON
            FloatingActionButton(
                onClick = {
                    val store = state.selectedStore
                    if (store != null) {
                        mapViewRef?.controller?.animateTo(GeoPoint(store.latitude, store.longitude))
                        Toast.makeText(context, "Centered on ${store.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Store location unavailable", Toast.LENGTH_SHORT).show()
                    }
                },
                containerColor = RouteCJNavySurface,
                contentColor = RouteCJCyanLight,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Storefront, contentDescription = "Store Location")
            }

            // MY LOCATION BUTTON
            FloatingActionButton(
                onClick = {
                    state.driverLocation?.let {
                        mapViewRef?.controller?.animateTo(it)
                        Toast.makeText(context, "Centered on Driver Location", Toast.LENGTH_SHORT).show()
                    } ?: run {
                        Toast.makeText(context, "Waiting for driver GPS location...", Toast.LENGTH_SHORT).show()
                    }
                },
                containerColor = Color.White,
                contentColor = RouteCJNavyDark,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "My Location")
            }

            // TRIP TARGET LOCATION BUTTON
            FloatingActionButton(
                onClick = {
                    state.targetPoint?.let {
                        mapViewRef?.controller?.animateTo(it)
                        Toast.makeText(context, "Centered on ${state.targetLabel}", Toast.LENGTH_SHORT).show()
                    } ?: run {
                        Toast.makeText(context, "No trip target location set", Toast.LENGTH_SHORT).show()
                    }
                },
                containerColor = RouteCJBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(imageVector = Icons.Default.GpsFixed, contentDescription = "Target Location")
            }
        }

        // Overlay 3: Bottom Action Navigation Panel
        if (state.tripDetails != null) {
            BottomNavigationPanel(
                state = state,
                onNavigateBackToTrip = onNavigateBackToTrip,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        } else if (state.selectedStore != null) {
            BottomStoreNavigationPanel(
                storeLocation = state.selectedStore,
                gpsState = state.gpsState,
                driverLocation = state.driverLocation,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun TopStoreInfoCard(
    storeLocation: StoreLocation,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = RouteCJCyanLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = storeLocation.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = RouteCJCyan.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "ACTIVE STORE",
                        color = RouteCJCyanLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = storeLocation.address,
                color = RouteCJTextSecondaryDark,
                fontSize = 12.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
fun BottomStoreNavigationPanel(
    storeLocation: StoreLocation,
    gpsState: DriverGpsState,
    driverLocation: GeoPoint?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(8.dp)
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
                        text = "STORE LOCATION TARGET",
                        color = RouteCJCyanLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = storeLocation.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val statusText = when (gpsState) {
                    is DriverGpsState.Active -> if (gpsState.isOffline) "OFFLINE" else if (gpsState.isLastKnownLocation) "CACHED" else "LIVE"
                    is DriverGpsState.Connecting -> "CONNECTING"
                    is DriverGpsState.WaitingForSignal -> "SIGNAL..."
                    else -> "INACTIVE"
                }
                val statusColor = when (gpsState) {
                    is DriverGpsState.Active -> if (gpsState.isOffline) RouteCJWarning else RouteCJSuccess
                    is DriverGpsState.Connecting, is DriverGpsState.WaitingForSignal -> RouteCJCyan
                    else -> RouteCJTextSecondaryDark
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "GPS $statusText",
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (driverLocation == null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Waiting for driver location for route...",
                    color = RouteCJWarning,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun LocationWarningBanner(
    gpsState: DriverGpsState,
    isRoutingFailed: Boolean = false,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, sub, color, icon) = when {
        isRoutingFailed -> Quadruple("ROUTE TEMPORARILY UNAVAILABLE", "Driving route could not be calculated. Live position tracking remains active.", RouteCJWarning, Icons.Default.Sync)
        gpsState is DriverGpsState.OfflineWaiting -> Quadruple("OFFLINE LOCAL", "Waiting for network connection...", RouteCJWarning, Icons.Default.CloudOff)
        gpsState is DriverGpsState.LocationDisabled -> Quadruple("LOCATION TURNED OFF", "Enable GPS to continue live trip tracking.", RouteCJError, Icons.Default.LocationOff)
        gpsState is DriverGpsState.PermissionRequired -> Quadruple("PERMISSION NEEDED", "Allow location access to track trip.", RouteCJError, Icons.Default.Security)
        gpsState is DriverGpsState.Connecting -> Quadruple("CONNECTING GPS", "Initializing location service...", RouteCJCyan, Icons.Default.Sync)
        gpsState is DriverGpsState.WaitingForSignal -> Quadruple("ACQUIRING SIGNAL", "Waiting for valid GPS coordinates...", RouteCJBlue, Icons.Default.GpsNotFixed)
        else -> Quadruple("", "", RouteCJNavyDark, Icons.Default.Info)
    }

    if (title.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = sub,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp
                    )
                }
            }

            if (gpsState is DriverGpsState.LocationDisabled || gpsState is DriverGpsState.PermissionRequired) {
                Button(
                    onClick = onOpenSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "FIX",
                        color = color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun BottomNavigationPanel(
    state: DriverMapUiState.Active,
    onNavigateBackToTrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trip = state.tripDetails ?: return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (state.isAtTarget) {
                val bannerTitle = if (state.targetLabel.contains("PICKUP")) "YOU HAVE ARRIVED AT PICKUP" else "YOU HAVE ARRIVED AT DESTINATION"
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = RouteCJSuccess.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RouteCJSuccess, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = bannerTitle,
                            color = RouteCJSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TARGET: ${state.targetLabel.uppercase()}",
                        color = RouteCJCyanLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    val distValue = state.distanceRemaining ?: state.routeResult?.distanceMeters
                    val durationValue = state.etaSeconds ?: state.routeResult?.durationSeconds

                    val distanceText = distValue?.let {
                        if (it < 1000) "${it.toInt()} m" else String.format("%.1f km", it / 1000.0)
                    } ?: "-- km"

                    val durationText = durationValue?.let { "${(it / 60).toInt()} min" } ?: "-- min"

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "DISTANCE REMAINING: $distanceText",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "ETA: $durationText",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ROUTE PROGRESS: ${state.routeProgress}%",
                            color = RouteCJTextSecondaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                }

                val statusText = when (state.gpsState) {
                    is DriverGpsState.Active -> if (state.gpsState.isOffline) "OFFLINE" else if (state.gpsState.isLastKnownLocation) "CACHED" else "LIVE"
                    is DriverGpsState.Connecting -> "CONNECTING"
                    is DriverGpsState.WaitingForSignal -> "SIGNAL..."
                    else -> "INACTIVE"
                }
                val statusColor = when (state.gpsState) {
                    is DriverGpsState.Active -> if (state.gpsState.isOffline) RouteCJWarning else RouteCJSuccess
                    is DriverGpsState.Connecting, is DriverGpsState.WaitingForSignal -> RouteCJCyan
                    else -> RouteCJTextSecondaryDark
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "GPS $statusText",
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (state.isAtTarget) {
                if (state.targetLabel.contains("PICKUP") && trip.driverArrived) {
                    // Do not show button if already arrived at pickup
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onNavigateBackToTrip,
                        colors = ButtonDefaults.buttonColors(containerColor = RouteCJSuccess),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        val btnText = if (state.targetLabel.contains("PICKUP")) "VERIFY CUSTOMER OTP" else "COMPLETE DELIVERY"
                        Text(
                            text = btnText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = RouteCJNavyDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopTripInfoCard(
    tripDetails: TripDetails,
    routeResult: OsrmRouteResult?,
    targetLabel: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(RouteCJSuccess)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tripDetails.orderNumber,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = RouteCJBlue.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = tripDetails.status.replace("_", " "),
                        color = RouteCJCyanLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${tripDetails.pickupAddress} → ${tripDetails.deliveryAddress}",
                color = RouteCJTextSecondaryDark,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}
