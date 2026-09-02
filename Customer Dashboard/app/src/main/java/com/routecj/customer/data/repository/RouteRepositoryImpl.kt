package com.routecj.customer.data.repository

import com.routecj.customer.domain.repository.RouteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class RouteRepositoryImpl @Inject constructor() : RouteRepository {

    override suspend fun getRoute(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): Result<List<Pair<Double, Double>>> = withContext(Dispatchers.IO) {
        try {
            // Coordinate validation
            if (originLat !in -90.0..90.0 || originLng !in -180.0..180.0 ||
                destLat !in -90.0..90.0 || destLng !in -180.0..180.0
            ) {
                return@withContext Result.failure(IllegalArgumentException("Invalid GPS coordinates"))
            }

            // OSRM format: longitude,latitude
            val urlString = "https://router.project-osrm.org/route/v1/driving/$originLng,$originLat;$destLng,$destLat?overview=full&geometries=geojson"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "RouteCJCustomerApp/1.0")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("OSRM HTTP Error: ${connection.responseCode}"))
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            val code = json.optString("code")

            if (code != "Ok") {
                return@withContext Result.failure(Exception("OSRM Route Error: $code"))
            }

            val routes = json.optJSONArray("routes")
            if (routes == null || routes.length() == 0) {
                return@withContext Result.failure(Exception("No routes found"))
            }

            val firstRoute = routes.getJSONObject(0)
            val geometry = firstRoute.getJSONObject("geometry")
            val coordinates = geometry.getJSONArray("coordinates")

            val points = mutableListOf<Pair<Double, Double>>()
            for (i in 0 until coordinates.length()) {
                val pointArray = coordinates.getJSONArray(i)
                val lng = pointArray.getDouble(0)
                val lat = pointArray.getDouble(1)
                points.add(Pair(lat, lng))
            }

            Result.success(points)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
