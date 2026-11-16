package com.manikanta.trekaltitudelogger.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrekDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrek(trek: TrekEntity): Long

    @Update
    suspend fun updateTrek(trek: TrekEntity)

    @Query("DELETE FROM treks WHERE id = :trekId")
    suspend fun deleteTrek(trekId: Long)

    @Query("SELECT * FROM treks ORDER BY startTime DESC")
    fun getAllTreksFlow(): Flow<List<TrekEntity>>

    @Query("SELECT * FROM treks ORDER BY startTime DESC")
    suspend fun getAllTreks(): List<TrekEntity>

    @Query("SELECT * FROM treks WHERE id = :trekId")
    suspend fun getTrekById(trekId: Long): TrekEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoint(waypoint: WaypointEntity): Long

    @Query("DELETE FROM waypoints WHERE id = :waypointId")
    suspend fun deleteWaypoint(waypointId: Long)

    @Query("SELECT * FROM waypoints WHERE trekId = :trekId ORDER BY timestamp ASC")
    fun getWaypointsForTrekFlow(trekId: Long): Flow<List<WaypointEntity>>

    @Query("SELECT * FROM waypoints WHERE trekId = :trekId ORDER BY timestamp ASC")
    suspend fun getWaypointsForTrek(trekId: Long): List<WaypointEntity>
}
