package com.manikanta.trekaltitudelogger.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "treks")
data class TrekEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startTime: Long,
    val endTime: Long = 0L,
    val totalDistanceKm: Double = 0.0,
    val totalClimbMeters: Double = 0.0,
    val highestPointMeters: Double = 0.0,
    val lowestPointMeters: Double = 0.0
)
