package com.manikanta.trekaltitudelogger.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "waypoints",
    foreignKeys = [
        ForeignKey(
            entity = TrekEntity::class,
            parentColumns = ["id"],
            childColumns = ["trekId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trekId"])]
)
data class WaypointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trekId: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double, // in meters
    val timestamp: Long
)
