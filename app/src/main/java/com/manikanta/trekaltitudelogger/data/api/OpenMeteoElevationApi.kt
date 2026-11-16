package com.manikanta.trekaltitudelogger.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object OpenMeteoElevationApi {

    suspend fun fetchElevation(latitude: Double, longitude: Double): Double? = withContext(Dispatchers.IO) {
        try {
            val urlString = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/elevation?latitude=%.6f&longitude=%.6f",
                latitude, longitude
            )
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 6000
            connection.readTimeout = 6000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(jsonText)
                if (jsonObject.has("elevation")) {
                    val elevationArray = jsonObject.getJSONArray("elevation")
                    if (elevationArray.length() > 0) {
                        return@withContext elevationArray.getDouble(0)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
