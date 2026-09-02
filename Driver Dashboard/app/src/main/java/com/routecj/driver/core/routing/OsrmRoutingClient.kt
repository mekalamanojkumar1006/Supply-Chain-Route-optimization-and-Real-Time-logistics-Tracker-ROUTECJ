package com.routecj.driver.core.routing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL

/**
 * Result of OSRM routing request.
 */
data class OsrmRouteResult(
    val points: List<GeoPoint>,
    val distanceMeters: Double,
    val durationSeconds: Double
)

/**
 * Open Source Routing Machine (OSRM) HTTP client.
 * Calls free public OSRM car routing endpoint to generate driving polylines.
 */
object OsrmRoutingClient {

    suspend fun getDrivingRoute(
        start: GeoPoint,
        destination: GeoPoint
    ): OsrmRouteResult? = withContext(Dispatchers.IO) {
        try {
            // OSRM coordinates format: {lon},{lat};{lon},{lat}
            val urlString = "https://router.project-osrm.org/route/v1/driving/" +
                    "${start.longitude},${start.latitude};${destination.longitude},${destination.latitude}" +
                    "?overview=full&geometries=geojson"

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "RouteCJDriver/1.0 (Android)")
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val code = json.optString("code")
                if (code == "Ok") {
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val routeObj = routes.getJSONObject(0)
                        val distance = routeObj.optDouble("distance", 0.0)
                        val duration = routeObj.optDouble("duration", 0.0)

                        val geometry = routeObj.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")

                        val geoPoints = mutableListOf<GeoPoint>()
                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            val lon = coord.getDouble(0)
                            val lat = coord.getDouble(1)
                            geoPoints.add(GeoPoint(lat, lon))
                        }

                        return@withContext OsrmRouteResult(
                            points = geoPoints,
                            distanceMeters = distance,
                            durationSeconds = duration
                        )
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
