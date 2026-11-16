package com.manikanta.trekaltitudelogger.data

import java.io.Serializable

data class Waypoint(
    val id: Long = 0,
    val trekId: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double, // in meters
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
) : Serializable
