package com.routecj.admin.presentation.tracking.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.routecj.admin.domain.model.DispatchStatus
import com.routecj.admin.domain.model.TrackingInfo
import com.routecj.admin.ui.theme.Primary
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * OpenStreetMap Compose Map wrapper with custom driver & destination markers,
 * smooth position updates, camera controls, and required OpenStreetMap attribution.
 */
@Composable
fun OsmTrackingMapView(
    trip: TrackingInfo,
    modifier: Modifier = Modifier,
    storeLocation: com.routecj.admin.domain.model.Godown? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val driverLat = trip.currentLatitude
    val driverLng = trip.currentLongitude
    val hasDriverGps = driverLat != null && driverLng != null && driverLat != 0.0 && driverLng != 0.0

    // Cache marker drawables to prevent recreation on every GPS frame
    val driverMarkerIcon = remember { createDriverMarkerDrawable(context) }
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

    // Lifecycle management for osmdroid (handles network thread and tile cache)
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

    // Driver Marker reference
    val driverMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = driverMarkerIcon
            title = "Driver: ${trip.driverName}"
            snippet = "Vehicle: ${trip.vehicleRegistration} (${trip.vehicleType})"
        }
    }

    // Store Marker reference
    val storeMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = destinationMarkerIcon // Using red marker for store too or a different color if needed
            // Actually let's use a different color for the store if possible, but for now red is fine
            title = storeLocation?.name ?: "RouteCJ Store"
            snippet = storeLocation?.address ?: "Vizianagaram Bus Complex"
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
            outlinePaint.color = android.graphics.Color.rgb(255, 179, 0)
            outlinePaint.strokeWidth = 6f
        }
    }

    // Update markers & Map camera
    LaunchedEffect(driverLat, driverLng, trip.driverName, trip.vehicleRegistration, storeLocation, trip.status) {
        // 1. Handle Store Marker (Always show if available)
        if (storeLocation != null && storeLocation.latitude != 0.0) {
            val storePoint = GeoPoint(storeLocation.latitude, storeLocation.longitude)
            storeMarker.position = storePoint
            storeMarker.title = storeLocation.name
            storeMarker.snippet = storeLocation.address
            if (!mapView.overlays.contains(storeMarker)) {
                mapView.overlays.add(storeMarker)
            }
        }

        // 2. Determine and Update Destination Marker
        val isGoingToStore = trip.status == DispatchStatus.ASSIGNED || 
                            trip.status == DispatchStatus.DISPATCH_CONFIRMED
        
        val destLat = if (isGoingToStore) storeLocation?.latitude else trip.destinationLatitude
        val destLng = if (isGoingToStore) storeLocation?.longitude else trip.destinationLongitude
        
        if (destLat != null && destLng != null && destLat != 0.0) {
            val destPoint = GeoPoint(destLat, destLng)
            destinationMarker.position = destPoint
            destinationMarker.title = if (isGoingToStore) "Pickup: ${storeLocation?.name}" else "Delivery: ${trip.customerName}"
            destinationMarker.snippet = if (isGoingToStore) storeLocation?.address else trip.deliveryLocation
            
            if (!mapView.overlays.contains(destinationMarker)) {
                mapView.overlays.add(destinationMarker)
            }

            // 3. Draw Route Line (Driver to Destination)
            if (hasDriverGps) {
                val routePoints = listOf(GeoPoint(driverLat!!, driverLng!!), destPoint)
                routeLine.setPoints(routePoints)
                if (!mapView.overlays.contains(routeLine)) {
                    mapView.overlays.add(0, routeLine) // Add at bottom
                }
            } else {
                mapView.overlays.remove(routeLine)
            }
        } else {
            mapView.overlays.remove(destinationMarker)
            mapView.overlays.remove(routeLine)
        }

        // 4. Handle Driver Marker
        if (hasDriverGps) {
            val point = GeoPoint(driverLat!!, driverLng!!)
            driverMarker.position = point
            driverMarker.title = "Driver: ${trip.driverName}"
            driverMarker.snippet = "Vehicle: ${trip.vehicleRegistration} (${trip.vehicleType}) • ${trip.speed.toInt()} km/h"

            if (!mapView.overlays.contains(driverMarker)) {
                mapView.overlays.add(driverMarker)
            }
            // Smoothly center the map on initial load or GPS movement
            mapView.controller.animateTo(point)
            mapView.invalidate()
        } else {
            mapView.overlays.remove(driverMarker)
            // If no driver GPS, center on destination or store
            if (destLat != null && destLng != null && destLat != 0.0) {
                mapView.controller.animateTo(GeoPoint(destLat, destLng))
            } else if (storeLocation != null && storeLocation.latitude != 0.0) {
                mapView.controller.animateTo(GeoPoint(storeLocation.latitude, storeLocation.longitude))
            }
            mapView.invalidate()
        }
    }

    Box(modifier = modifier) {
        // 1. AndroidView holding the MapView
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Floating Map Camera Controls (Zoom In, Zoom Out, Recenter)
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
                    if (hasDriverGps) {
                        mapView.controller.animateTo(GeoPoint(driverLat!!, driverLng!!))
                    } else if (storeLocation != null && storeLocation.latitude != 0.0) {
                        mapView.controller.animateTo(GeoPoint(storeLocation.latitude, storeLocation.longitude))
                    } else {
                        // Fallback Vizianagaram Bus Complex coordinates if no database data yet
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

        // 3. Location Status Banner if GPS is unavailable
        if (!hasDriverGps) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 16.dp, end = 64.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.95f),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFF59E0B), CircleShape))
                    Text(
                        text = "Driver GPS location unavailable",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // 4. REQUIRED OpenStreetMap Attribution
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
 * Creates a high-visibility vector driver icon with a pulsing outer ring.
 */
private fun createDriverMarkerDrawable(context: Context): BitmapDrawable {
    val sizePx = (48 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val center = sizePx / 2f
    val outerRadius = sizePx * 0.44f
    val innerRadius = sizePx * 0.32f
    val coreRadius = sizePx * 0.16f

    // Outer glow / halo
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(70, 255, 179, 0)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, outerRadius, haloPaint)

    // Primary circle
    val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(255, 179, 0) // Primary Gold
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, innerRadius, primaryPaint)

    // Border
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3 * context.resources.displayMetrics.density
    }
    canvas.drawCircle(center, center, innerRadius, strokePaint)

    // Inner core dot
    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, coreRadius, corePaint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Creates a destination marker icon.
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
