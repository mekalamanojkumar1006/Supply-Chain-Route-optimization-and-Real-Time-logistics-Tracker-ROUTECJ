package com.routecj.admin.presentation.tracking.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.routecj.admin.R
import com.routecj.admin.domain.model.DispatchStatus
import com.routecj.admin.domain.model.Godown
import com.routecj.admin.domain.model.TrackingInfo
import com.routecj.admin.presentation.components.PremiumStatusChip
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.*

/**
 * OpenStreetMap Fleet & Single-Trip Live Tracking Map.
 * Features:
 * - Real-time Truck Vector Markers with driver heading rotation
 * - Route CJ branding (Navy & Cyan)
 * - Single trip and Multi-driver active fleet tracking
 * - Driver Info Card with Speed, Accuracy, Order, Destination & Direct Dialer
 * - "Navigate / Get Direction" live route polyline calculation
 * - Store / Godown destination marker (Vizianagaram Bus Complex)
 * - GPS Stale & Offline handling
 */
@Composable
fun OsmTrackingMapView(
    modifier: Modifier = Modifier,
    trip: TrackingInfo? = null,
    allTrips: List<TrackingInfo> = emptyList(),
    storeLocation: Godown? = null,
    onTripSelected: ((TrackingInfo) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Consolidate trips to render (either single selected trip or all active trips)
    val tripsToRender = remember(trip, allTrips) {
        if (trip != null) listOf(trip) else allTrips
    }

    // State for currently inspected driver telemetry card overlay
    var inspectedTrip by remember { mutableStateOf<TrackingInfo?>(trip) }

    // Update inspected trip when selected trip prop changes
    LaunchedEffect(trip) {
        inspectedTrip = trip
    }

    // Cache destination/store marker icon
    val storeMarkerIcon = remember { createStoreMarkerDrawable(context) }
    val destinationMarkerIcon = remember { createDestinationMarkerDrawable(context) }

    // MapView instance retained across recompositions
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 4.0
            maxZoomLevel = 20.0
            controller.setZoom(15.0)
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

    // Marker collections reference
    val driverMarkersMap = remember { mutableMapOf<String, Marker>() }

    // Store Marker reference
    val storeMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = storeMarkerIcon
        }
    }

    // Destination Marker reference
    val destinationMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = destinationMarkerIcon
        }
    }

    // Route Polyline reference
    val routeLine = remember {
        Polyline(mapView).apply {
            outlinePaint.color = android.graphics.Color.rgb(0, 199, 199) // RouteCJ Cyan
            outlinePaint.strokeWidth = 8f
        }
    }

    // Helper to frame driver & destination on map
    val zoomToRoute = remember(mapView, storeLocation) {
        { targetTrip: TrackingInfo ->
            val dLat = targetTrip.currentLatitude
            val dLng = targetTrip.currentLongitude
            val hasGps = dLat != null && dLng != null && dLat != 0.0 && dLng != 0.0

            val isGoingToStore = targetTrip.status == DispatchStatus.ASSIGNED ||
                    targetTrip.status == DispatchStatus.DISPATCH_CONFIRMED

            val destLat = if (isGoingToStore) storeLocation?.latitude else targetTrip.destinationLatitude
            val destLng = if (isGoingToStore) storeLocation?.longitude else targetTrip.destinationLongitude

            if (hasGps && destLat != null && destLng != null && destLat != 0.0) {
                val driverPoint = GeoPoint(dLat!!, dLng!!)
                val destPoint = GeoPoint(destLat, destLng)

                routeLine.setPoints(listOf(driverPoint, destPoint))
                if (!mapView.overlays.contains(routeLine)) {
                    mapView.overlays.add(0, routeLine)
                }

                try {
                    val bbox = BoundingBox.fromGeoPoints(listOf(driverPoint, destPoint))
                    mapView.zoomToBoundingBox(bbox, true, 140)
                } catch (_: Exception) {
                    mapView.controller.animateTo(driverPoint)
                }
            } else if (hasGps) {
                mapView.controller.animateTo(GeoPoint(dLat!!, dLng!!))
            }
        }
    }

    // Realtime Markers & Overlays Update
    LaunchedEffect(tripsToRender, storeLocation, inspectedTrip) {
        // 1. Store Marker (Vizianagaram Bus Complex)
        if (storeLocation != null && storeLocation.latitude != 0.0) {
            val storePoint = GeoPoint(storeLocation.latitude, storeLocation.longitude)
            storeMarker.position = storePoint
            storeMarker.title = storeLocation.name
            storeMarker.snippet = storeLocation.address
            if (!mapView.overlays.contains(storeMarker)) {
                mapView.overlays.add(storeMarker)
            }
        } else {
            mapView.overlays.remove(storeMarker)
        }

        // 2. Active Driver Truck Markers
        val activeDispatchIds = tripsToRender.map { it.dispatchId }.toSet()

        // Clean up drivers no longer active
        val iterator = driverMarkersMap.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!activeDispatchIds.contains(entry.key)) {
                mapView.overlays.remove(entry.value)
                iterator.remove()
            }
        }

        var focusedPoint: GeoPoint? = null

        tripsToRender.forEach { activeTrip ->
            val dLat = activeTrip.currentLatitude
            val dLng = activeTrip.currentLongitude
            val hasGps = dLat != null && dLng != null && dLat != 0.0 && dLng != 0.0

            if (hasGps) {
                val point = GeoPoint(dLat!!, dLng!!)
                if (activeTrip == trip || activeTrip == inspectedTrip) {
                    focusedPoint = point
                }

                val marker = driverMarkersMap.getOrPut(activeTrip.dispatchId) {
                    Marker(mapView).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        mapView.overlays.add(this)
                    }
                }

                marker.position = point
                marker.icon = getTruckMarkerBitmapDrawable(context, isStale = activeTrip.isLocationStale)
                marker.rotation = if (activeTrip.heading > 0) activeTrip.heading.toFloat() else 0f
                marker.title = "🚚 ${activeTrip.driverName}"
                marker.snippet = "Vehicle: ${activeTrip.vehicleRegistration} • ${activeTrip.speed.toInt()} km/h"
                marker.alpha = if (activeTrip.isLocationStale) 0.6f else 1.0f

                marker.setOnMarkerClickListener { _, _ ->
                    inspectedTrip = activeTrip
                    onTripSelected?.invoke(activeTrip)
                    zoomToRoute(activeTrip)
                    true
                }
            } else {
                driverMarkersMap[activeTrip.dispatchId]?.let {
                    mapView.overlays.remove(it)
                    driverMarkersMap.remove(activeTrip.dispatchId)
                }
            }
        }

        // 3. Destination Marker & Route Line for Inspected Trip
        val currentInspect = inspectedTrip ?: trip
        if (currentInspect != null) {
            val dLat = currentInspect.currentLatitude
            val dLng = currentInspect.currentLongitude
            val hasGps = dLat != null && dLng != null && dLat != 0.0 && dLng != 0.0

            val isGoingToStore = currentInspect.status == DispatchStatus.ASSIGNED ||
                    currentInspect.status == DispatchStatus.DISPATCH_CONFIRMED

            val destLat = if (isGoingToStore) storeLocation?.latitude else currentInspect.destinationLatitude
            val destLng = if (isGoingToStore) storeLocation?.longitude else currentInspect.destinationLongitude

            if (destLat != null && destLng != null && destLat != 0.0) {
                val destPoint = GeoPoint(destLat, destLng)
                destinationMarker.position = destPoint
                destinationMarker.title = if (isGoingToStore) "Pickup: ${storeLocation?.name}" else "Delivery: ${currentInspect.customerName}"
                destinationMarker.snippet = if (isGoingToStore) storeLocation?.address else currentInspect.deliveryLocation

                if (!mapView.overlays.contains(destinationMarker)) {
                    mapView.overlays.add(destinationMarker)
                }

                if (hasGps) {
                    val routePoints = listOf(GeoPoint(dLat!!, dLng!!), destPoint)
                    routeLine.setPoints(routePoints)
                    if (!mapView.overlays.contains(routeLine)) {
                        mapView.overlays.add(0, routeLine)
                    }
                } else {
                    mapView.overlays.remove(routeLine)
                }
            } else {
                mapView.overlays.remove(destinationMarker)
                mapView.overlays.remove(routeLine)
            }
        } else {
            mapView.overlays.remove(destinationMarker)
            mapView.overlays.remove(routeLine)
        }

        // Initial camera animation
        if (focusedPoint != null) {
            mapView.controller.animateTo(focusedPoint)
        } else if (storeLocation != null && storeLocation.latitude != 0.0) {
            mapView.controller.animateTo(GeoPoint(storeLocation.latitude, storeLocation.longitude))
        }

        mapView.invalidate()
    }

    Box(modifier = modifier) {
        // 1. Map View
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Map Camera Floating Actions
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Recenter Button
            FloatingMapButton(
                icon = Icons.Default.MyLocation,
                contentDescription = "Recenter",
                onClick = {
                    val activeTarget = inspectedTrip ?: trip
                    val dLat = activeTarget?.currentLatitude
                    val dLng = activeTarget?.currentLongitude
                    if (dLat != null && dLng != null && dLat != 0.0) {
                        mapView.controller.animateTo(GeoPoint(dLat, dLng))
                    } else if (storeLocation != null && storeLocation.latitude != 0.0) {
                        mapView.controller.animateTo(GeoPoint(storeLocation.latitude, storeLocation.longitude))
                    } else {
                        mapView.controller.animateTo(GeoPoint(18.1124436, 83.3986427))
                    }
                }
            )

            // Zoom In Button
            FloatingMapButton(
                icon = Icons.Default.Add,
                contentDescription = "Zoom In",
                onClick = { mapView.controller.zoomIn() }
            )

            // Zoom Out Button
            FloatingMapButton(
                icon = Icons.Default.Remove,
                contentDescription = "Zoom Out",
                onClick = { mapView.controller.zoomOut() }
            )
        }

        // 3. Inspected Driver Telemetry Panel Card (Floating Overlay)
        if (inspectedTrip != null) {
            val active = inspectedTrip!!
            val dLat = active.currentLatitude
            val dLng = active.currentLongitude
            val hasGps = dLat != null && dLng != null && dLat != 0.0 && dLng != 0.0

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(14.dp)
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Title Bar with Driver & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00C7C7).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🚚", fontSize = 18.sp)
                            }
                            Column {
                                Text(
                                    text = active.driverName,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Vehicle: ${active.vehicleRegistration} (${active.vehicleType})",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            }
                        }

                        IconButton(onClick = { inspectedTrip = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF334155))

                    // Status & Telemetry Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("STATUS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF00C7C7))
                            Text(
                                text = active.status.name.replace("_", " "),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Column {
                            Text("SPEED", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF00C7C7))
                            Text(
                                text = "${active.speed.toInt()} km/h",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Column {
                            Text("ORDER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF00C7C7))
                            Text(
                                text = "#${active.orderNumber}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Destination Address
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Text(
                            text = "Dest: ${active.deliveryLocation}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.LightGray,
                            maxLines = 1
                        )
                    }

                    // Action Buttons (Navigate & Call)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // "Navigate / Get Direction" Button
                        Button(
                            onClick = { zoomToRoute(active) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C7C7)),
                            enabled = hasGps
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Navigate Route", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        // Call Driver Button
                        if (active.driverPhone.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${active.driverPhone}"))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00C7C7))
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF00C7C7), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // 4. Required OpenStreetMap Attribution
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 4.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color.Black.copy(alpha = 0.65f)
        ) {
            Text(
                text = "© OpenStreetMap contributors",
                fontSize = 10.sp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun FloatingMapButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color(0xFF1E293B).copy(alpha = 0.9f),
        shadowElevation = 6.dp,
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Creates a crisp BitmapDrawable for active trucks from the vector asset `ic_routecj_truck_marker.xml`.
 */
fun getTruckMarkerBitmapDrawable(context: Context, isStale: Boolean = false): BitmapDrawable {
    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_routecj_truck_marker)
        ?: return createFallbackTruckBitmapDrawable(context)

    val sizePx = (52 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    drawable.setBounds(0, 0, sizePx, sizePx)
    if (isStale) {
        drawable.alpha = 140
    } else {
        drawable.alpha = 255
    }
    drawable.draw(canvas)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createFallbackTruckBitmapDrawable(context: Context): BitmapDrawable {
    val sizePx = (48 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val center = sizePx / 2f
    val radius = sizePx * 0.38f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(0, 199, 199)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, radius, paint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Creates a Store / Godown destination marker drawable.
 */
private fun createStoreMarkerDrawable(context: Context): BitmapDrawable {
    val sizePx = (44 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val center = sizePx / 2f
    val radius = sizePx * 0.36f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(11, 23, 42) // Deep Navy
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, radius, paint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(0, 199, 199) // Cyan Border
        style = Paint.Style.STROKE
        strokeWidth = 3f * context.resources.displayMetrics.density
    }
    canvas.drawCircle(center, center, radius, strokePaint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Creates a customer delivery destination marker icon.
 */
private fun createDestinationMarkerDrawable(context: Context): BitmapDrawable {
    val sizePx = (40 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val center = sizePx / 2f
    val radius = sizePx * 0.36f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(239, 68, 68) // Red
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, radius, paint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * context.resources.displayMetrics.density
    }
    canvas.drawCircle(center, center, radius, strokePaint)

    return BitmapDrawable(context.resources, bitmap)
}
