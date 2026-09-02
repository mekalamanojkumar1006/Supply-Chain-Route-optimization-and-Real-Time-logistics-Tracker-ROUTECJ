package com.routecj.customer.data.repository

import com.routecj.customer.domain.model.GeocodingResult
import com.routecj.customer.domain.repository.GeocodingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject

class NominatimGeocodingRepositoryImpl @Inject constructor() : GeocodingRepository {

    override suspend fun searchAddress(query: String): Result<List<GeocodingResult>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext Result.success(emptyList())
        }

        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val urlString = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=5&addressdetails=1"
            val url = URL(urlString)

            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "RouteCJCustomerApp/1.0 (contact@routecj.com)")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("HTTP ${connection.responseCode}: Geocoding service error"))
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(responseText)

            if (jsonArray.length() == 0) {
                return@withContext Result.failure(Exception("Couldn't find that location"))
            }

            val results = mutableListOf<GeocodingResult>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val displayName = item.optString("display_name")
                val lat = item.optDouble("lat", Double.NaN)
                val lon = item.optDouble("lon", Double.NaN)

                if (!lat.isNaN() && !lon.isNaN() && displayName.isNotBlank()) {
                    results.add(
                        GeocodingResult(
                            displayName = displayName,
                            latitude = lat,
                            longitude = lon
                        )
                    )
                }
            }

            if (results.isEmpty()) {
                Result.failure(Exception("Couldn't find that location"))
            } else {
                Result.success(results)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Couldn't find that location. Please try again."))
        }
    }
}
