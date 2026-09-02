package com.routecj.customer.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.preference.PreferenceManager
import com.routecj.customer.domain.model.DriverLocation
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun RouteCJMap(
    modifier: Modifier = Modifier,
    pickupLocation: Pair<Double, Double>? = null,
    destinationLocation: Pair<Double, Double>? = null,
    driverLocation: DriverLocation? = null,
    godownLocation: Pair<Double, Double>? = null,
    routePoints: List<Pair<Double, Double>> = emptyList(),
    showDriver: Boolean = true,
    showRoute: Boolean = true,
    routeError: String? = null,
    zoomLevel: Double = 15.0,
    enableZoomControls: Boolean = true,
    onPickupLocationChanged: ((Double, Double) -> Unit)? = null,
    onDestinationLocationChanged: ((Double, Double) -> Unit)? = null,
    onMapClick: ((Pair<Double, Double>) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize osmdroid configuration once
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            PreferenceManager.getDefaultSharedPreferences(context)
        )
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val secondaryColor = MaterialTheme.colorScheme.secondary.toArgb()
    val errorColor = MaterialTheme.colorScheme.error.toArgb()
    val tertiaryColor = MaterialTheme.colorScheme.tertiary.toArgb()

    // Native crisp vector pin drawables
    val pickupIcon = remember(primaryColor) { createPinDrawable(context, primaryColor) }
    val destIcon = remember(errorColor) { createPinDrawable(context, errorColor) }
    val godownIcon = remember(tertiaryColor) { createPinDrawable(context, tertiaryColor) }
    val truckIcon = remember(secondaryColor) { createTruckDrawable(context, secondaryColor) }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var lastLocationKey by remember { mutableStateOf("") }

    val currentLocationKey = "${pickupLocation?.first}_${pickupLocation?.second}_${destinationLocation?.first}_${destinationLocation?.second}_${godownLocation?.first}_${godownLocation?.second}"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    minZoomLevel = 4.0
                    maxZoomLevel = 20.0
                    controller.setZoom(zoomLevel)
                    mapViewRef = this
                }
            },
            update = { mapView ->
                mapView.overlays.clear()

                // Map Click Events Overlay
                if (onMapClick != null) {
                    val mapEventsReceiver = object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            if (p != null) {
                                onMapClick.invoke(Pair(p.latitude, p.longitude))
                                return true
                            }
                            return false
                        }

                        override fun longPressHelper(p: GeoPoint?): Boolean = false
                    }
                    mapView.overlays.add(0, MapEventsOverlay(mapEventsReceiver))
                }

                val activeGeoPoints = mutableListOf<GeoPoint>()

                // 1. Pickup Marker (Draggable if callback provided)
                if (pickupLocation != null && isValidCoordinate(pickupLocation.first, pickupLocation.second)) {
                    val pGeo = GeoPoint(pickupLocation.first, pickupLocation.second)
                    activeGeoPoints.add(pGeo)

                    val pMarker = Marker(mapView).apply {
                        position = pGeo
                        title = "Pickup Point (Drag or Tap Map to Move)"
                        icon = pickupIcon
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        if (onPickupLocationChanged != null) {
                            isDraggable = true
                            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                                override fun onMarkerDrag(marker: Marker?) {}
                                override fun onMarkerDragStart(marker: Marker?) {}
                                override fun onMarkerDragEnd(marker: Marker?) {
                                    if (marker != null) {
                                        onPickupLocationChanged.invoke(marker.position.latitude, marker.position.longitude)
                                    }
                                }
                            })
                        }
                    }
                    mapView.overlays.add(pMarker)
                }

                // 2. Destination Marker (Draggable if callback provided)
                if (destinationLocation != null && isValidCoordinate(destinationLocation.first, destinationLocation.second)) {
                    val dGeo = GeoPoint(destinationLocation.first, destinationLocation.second)
                    activeGeoPoints.add(dGeo)

                    val dMarker = Marker(mapView).apply {
                        position = dGeo
                        title = "Destination"
                        icon = destIcon
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        if (onDestinationLocationChanged != null) {
                            isDraggable = true
                            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                                override fun onMarkerDrag(marker: Marker?) {}
                                override fun onMarkerDragStart(marker: Marker?) {}
                                override fun onMarkerDragEnd(marker: Marker?) {
                                    if (marker != null) {
                                        onDestinationLocationChanged.invoke(marker.position.latitude, marker.position.longitude)
                                    }
                                }
                            })
                        }
                    }
                    mapView.overlays.add(dMarker)
                }

                // 3. Godown Marker
                if (godownLocation != null && isValidCoordinate(godownLocation.first, godownLocation.second)) {
                    val gGeo = GeoPoint(godownLocation.first, godownLocation.second)
                    activeGeoPoints.add(gGeo)

                    val gMarker = Marker(mapView).apply {
                        position = gGeo
                        title = "Warehouse / Godown"
                        icon = godownIcon
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    mapView.overlays.add(gMarker)
                }

                // 4. Live Driver Marker
                if (showDriver && driverLocation != null && isValidCoordinate(driverLocation.latitude, driverLocation.longitude)) {
                    val drGeo = GeoPoint(driverLocation.latitude, driverLocation.longitude)
                    activeGeoPoints.add(drGeo)

                    val drMarker = Marker(mapView).apply {
                        position = drGeo
                        title = "Driver Location"
                        icon = truckIcon
                        rotation = driverLocation.heading
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    mapView.overlays.add(drMarker)
                }

                // 5. Route Polyline
                if (showRoute && routePoints.size >= 2) {
                    val polyline = Polyline().apply {
                        outlinePaint.color = primaryColor
                        outlinePaint.strokeWidth = 10f
                        setPoints(routePoints.map { GeoPoint(it.first, it.second) })
                    }
                    mapView.overlays.add(polyline)
                }

                // Camera fitting logic — ONLY run when locations change, NOT on every zoom/pan gesture!
                if (currentLocationKey != lastLocationKey) {
                    lastLocationKey = currentLocationKey
                    mapView.post {
                        if (activeGeoPoints.size > 1) {
                            try {
                                val boundingBox = BoundingBox.fromGeoPoints(activeGeoPoints)
                                mapView.zoomToBoundingBox(boundingBox, true, 100)
                            } catch (e: Exception) {
                                mapView.controller.setCenter(activeGeoPoints.first())
                                mapView.controller.setZoom(zoomLevel)
                            }
                        } else if (activeGeoPoints.size == 1) {
                            mapView.controller.setCenter(activeGeoPoints.first())
                            mapView.controller.setZoom(zoomLevel)
                        }
                    }
                }

                mapView.invalidate()
            }
        )

        // Lifecycle observer to handle MapView resume/pause/detach
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                    Lifecycle.Event.ON_DESTROY -> mapViewRef?.onDetach()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        // Routing failure fallback warning overlay
        if (routeError != null && showRoute) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "⚠️ Route temporarily unavailable",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // On-screen Zoom (+), Zoom (-), and Recenter (🎯) Controls
        if (enableZoomControls) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Zoom In
                SmallFloatingActionButton(
                    onClick = { mapViewRef?.controller?.zoomIn() },
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                    modifier = Modifier.size(38.dp)
                ) {
                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                // Zoom Out
                SmallFloatingActionButton(
                    onClick = { mapViewRef?.controller?.zoomOut() },
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                    modifier = Modifier.size(38.dp)
                ) {
                    Text("-", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }

                // Recenter
                SmallFloatingActionButton(
                    onClick = {
                        val points = mutableListOf<GeoPoint>()
                        pickupLocation?.let { points.add(GeoPoint(it.first, it.second)) }
                        destinationLocation?.let { points.add(GeoPoint(it.first, it.second)) }
                        driverLocation?.let { points.add(GeoPoint(it.latitude, it.longitude)) }
                        if (points.size > 1) {
                            try {
                                mapViewRef?.zoomToBoundingBox(BoundingBox.fromGeoPoints(points), true, 100)
                            } catch (e: Exception) {
                                mapViewRef?.controller?.setCenter(points.first())
                            }
                        } else if (points.size == 1) {
                            mapViewRef?.controller?.setCenter(points.first())
                            mapViewRef?.controller?.setZoom(zoomLevel)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(38.dp)
                ) {
                    Text("🎯", fontSize = 16.sp)
                }
            }
        }
    }
}

private fun isValidCoordinate(lat: Double, lng: Double): Boolean {
    return lat in -90.0..90.0 && lng in -180.0..180.0 && (lat != 0.0 || lng != 0.0)
}

private fun createPinDrawable(context: Context, pinColor: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val width = (30 * density).toInt()
    val height = (38 * density).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Drop shadow
    paint.color = 0x40000000
    canvas.drawCircle(width / 2f, height - (4 * density), 6 * density, paint)

    // Pin head (circle)
    paint.color = pinColor
    paint.style = Paint.Style.FILL
    val radius = 12 * density
    val centerX = width / 2f
    val centerY = radius + (2 * density)
    canvas.drawCircle(centerX, centerY, radius, paint)

    // Pin point (teardrop bottom)
    val path = Path().apply {
        moveTo(centerX - (8 * density), centerY + (6 * density))
        lineTo(centerX, height - (6 * density))
        lineTo(centerX + (8 * density), centerY + (6 * density))
        close()
    }
    canvas.drawPath(path, paint)

    // Inner white dot
    paint.color = 0xFFFFFFFF.toInt()
    canvas.drawCircle(centerX, centerY, 4.5f * density, paint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createTruckDrawable(context: Context, bgColor: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (34 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Drop shadow
    paint.color = 0x40000000
    canvas.drawCircle(size / 2f, size / 2f + (2 * density), (size / 2f) - (2 * density), paint)

    // Circle background
    paint.color = bgColor
    paint.style = Paint.Style.FILL
    val center = size / 2f
    val radius = (size / 2f) - (3 * density)
    canvas.drawCircle(center, center, radius, paint)

    // White border
    paint.color = 0xFFFFFFFF.toInt()
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2 * density
    canvas.drawCircle(center, center, radius, paint)

    // Truck emoji
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 18 * density
        textAlign = Paint.Align.CENTER
    }
    val fontMetrics = textPaint.fontMetrics
    val y = center - (fontMetrics.ascent + fontMetrics.descent) / 2f
    canvas.drawText("🚚", center, y, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}
