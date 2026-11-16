package com.manikanta.trekaltitudelogger.data

import java.util.Locale
import kotlin.math.roundToInt

object UnitFormatter {

    var useImperial: Boolean = false

    fun formatAltitude(meters: Double): String {
        return if (useImperial) {
            val feet = meters * 3.28084
            String.format(Locale.US, "%.0f ft", feet)
        } else {
            String.format(Locale.US, "%.0f m", meters)
        }
    }

    fun formatClimb(meters: Double): String {
        return if (useImperial) {
            val feet = meters * 3.28084
            String.format(Locale.US, "+%.0f ft", feet)
        } else {
            String.format(Locale.US, "+%.0f m", meters)
        }
    }

    fun formatDescent(meters: Double): String {
        return if (useImperial) {
            val feet = meters * 3.28084
            String.format(Locale.US, "-%.0f ft", feet)
        } else {
            String.format(Locale.US, "-%.0f m", meters)
        }
    }

    fun formatDistance(meters: Double): String {
        return if (useImperial) {
            val miles = meters / 1609.34
            if (miles < 0.1) {
                val feet = meters * 3.28084
                String.format(Locale.US, "%.0f ft", feet)
            } else {
                String.format(Locale.US, "%.2f mi", miles)
            }
        } else {
            if (meters < 1000) {
                String.format(Locale.US, "%.0f m", meters)
            } else {
                val km = meters / 1000.0
                String.format(Locale.US, "%.2f km", km)
            }
        }
    }

    fun formatDuration(durationMillis: Long): String {
        val totalSeconds = durationMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatCoordinates(lat: Double, lon: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lonDir = if (lon >= 0) "E" else "W"
        return String.format(Locale.US, "%.5f°%s, %.5f°%s", Math.abs(lat), latDir, Math.abs(lon), lonDir)
    }
}
