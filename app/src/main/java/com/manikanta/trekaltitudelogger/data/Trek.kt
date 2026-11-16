package com.manikanta.trekaltitudelogger.data

import java.io.Serializable

data class Trek(
    val id: Long = 0,
    val title: String,
    val startTime: Long,
    val endTime: Long = 0L,
    val totalDistance: Double = 0.0, // in meters
    val totalClimb: Double = 0.0, // total elevation gain in meters
    val totalDescent: Double = 0.0, // total elevation loss in meters
    val minAltitude: Double = 0.0, // in meters
    val maxAltitude: Double = 0.0, // in meters
    val waypointCount: Int = 0,
    val notes: String = ""
) : Serializable
