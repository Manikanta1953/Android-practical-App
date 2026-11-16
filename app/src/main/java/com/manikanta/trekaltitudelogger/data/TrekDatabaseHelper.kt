package com.manikanta.trekaltitudelogger.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TrekDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "trek_altitude_logger.db"
        private const val DATABASE_VERSION = 1

        // Treks table
        private const val TABLE_TREKS = "treks"
        private const val KEY_TREK_ID = "id"
        private const val KEY_TREK_TITLE = "title"
        private const val KEY_TREK_START_TIME = "start_time"
        private const val KEY_TREK_END_TIME = "end_time"
        private const val KEY_TREK_TOTAL_DISTANCE = "total_distance"
        private const val KEY_TREK_TOTAL_CLIMB = "total_climb"
        private const val KEY_TREK_TOTAL_DESCENT = "total_descent"
        private const val KEY_TREK_MIN_ALTITUDE = "min_altitude"
        private const val KEY_TREK_MAX_ALTITUDE = "max_altitude"
        private const val KEY_TREK_WAYPOINT_COUNT = "waypoint_count"
        private const val KEY_TREK_NOTES = "notes"

        // Waypoints table
        private const val TABLE_WAYPOINTS = "waypoints"
        private const val KEY_WP_ID = "id"
        private const val KEY_WP_TREK_ID = "trek_id"
        private const val KEY_WP_NAME = "name"
        private const val KEY_WP_LATITUDE = "latitude"
        private const val KEY_WP_LONGITUDE = "longitude"
        private const val KEY_WP_ALTITUDE = "altitude"
        private const val KEY_WP_TIMESTAMP = "timestamp"
        private const val KEY_WP_NOTES = "notes"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTreksTable = ("CREATE TABLE " + TABLE_TREKS + " ("
                + KEY_TREK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + KEY_TREK_TITLE + " TEXT, "
                + KEY_TREK_START_TIME + " INTEGER, "
                + KEY_TREK_END_TIME + " INTEGER, "
                + KEY_TREK_TOTAL_DISTANCE + " REAL, "
                + KEY_TREK_TOTAL_CLIMB + " REAL, "
                + KEY_TREK_TOTAL_DESCENT + " REAL, "
                + KEY_TREK_MIN_ALTITUDE + " REAL, "
                + KEY_TREK_MAX_ALTITUDE + " REAL, "
                + KEY_TREK_WAYPOINT_COUNT + " INTEGER, "
                + KEY_TREK_NOTES + " TEXT" + ")")

        val createWaypointsTable = ("CREATE TABLE " + TABLE_WAYPOINTS + " ("
                + KEY_WP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + KEY_WP_TREK_ID + " INTEGER, "
                + KEY_WP_NAME + " TEXT, "
                + KEY_WP_LATITUDE + " REAL, "
                + KEY_WP_LONGITUDE + " REAL, "
                + KEY_WP_ALTITUDE + " REAL, "
                + KEY_WP_TIMESTAMP + " INTEGER, "
                + KEY_WP_NOTES + " TEXT, "
                + "FOREIGN KEY(" + KEY_WP_TREK_ID + ") REFERENCES " + TABLE_TREKS + "(" + KEY_TREK_ID + ") ON DELETE CASCADE" + ")")

        db.execSQL(createTreksTable)
        db.execSQL(createWaypointsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WAYPOINTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TREKS")
        onCreate(db)
    }

    // --- Trek Operations ---

    fun insertTrek(trek: Trek): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_TREK_TITLE, trek.title)
            put(KEY_TREK_START_TIME, trek.startTime)
            put(KEY_TREK_END_TIME, trek.endTime)
            put(KEY_TREK_TOTAL_DISTANCE, trek.totalDistance)
            put(KEY_TREK_TOTAL_CLIMB, trek.totalClimb)
            put(KEY_TREK_TOTAL_DESCENT, trek.totalDescent)
            put(KEY_TREK_MIN_ALTITUDE, trek.minAltitude)
            put(KEY_TREK_MAX_ALTITUDE, trek.maxAltitude)
            put(KEY_TREK_WAYPOINT_COUNT, trek.waypointCount)
            put(KEY_TREK_NOTES, trek.notes)
        }
        return db.insert(TABLE_TREKS, null, values)
    }

    fun updateTrek(trek: Trek): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_TREK_TITLE, trek.title)
            put(KEY_TREK_START_TIME, trek.startTime)
            put(KEY_TREK_END_TIME, trek.endTime)
            put(KEY_TREK_TOTAL_DISTANCE, trek.totalDistance)
            put(KEY_TREK_TOTAL_CLIMB, trek.totalClimb)
            put(KEY_TREK_TOTAL_DESCENT, trek.totalDescent)
            put(KEY_TREK_MIN_ALTITUDE, trek.minAltitude)
            put(KEY_TREK_MAX_ALTITUDE, trek.maxAltitude)
            put(KEY_TREK_WAYPOINT_COUNT, trek.waypointCount)
            put(KEY_TREK_NOTES, trek.notes)
        }
        return db.update(TABLE_TREKS, values, "$KEY_TREK_ID = ?", arrayOf(trek.id.toString()))
    }

    fun deleteTrek(trekId: Long): Int {
        val db = this.writableDatabase
        db.delete(TABLE_WAYPOINTS, "$KEY_WP_TREK_ID = ?", arrayOf(trekId.toString()))
        return db.delete(TABLE_TREKS, "$KEY_TREK_ID = ?", arrayOf(trekId.toString()))
    }

    fun getTrek(trekId: Long): Trek? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_TREKS, null, "$KEY_TREK_ID = ?",
            arrayOf(trekId.toString()), null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                Trek(
                    id = it.getLong(it.getColumnIndexOrThrow(KEY_TREK_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(KEY_TREK_TITLE)),
                    startTime = it.getLong(it.getColumnIndexOrThrow(KEY_TREK_START_TIME)),
                    endTime = it.getLong(it.getColumnIndexOrThrow(KEY_TREK_END_TIME)),
                    totalDistance = it.getDouble(it.getColumnIndexOrThrow(KEY_TREK_TOTAL_DISTANCE)),
                    totalClimb = it.getDouble(it.getColumnIndexOrThrow(KEY_TREK_TOTAL_CLIMB)),
                    totalDescent = it.getDouble(it.getColumnIndexOrThrow(KEY_TREK_TOTAL_DESCENT)),
                    minAltitude = it.getDouble(it.getColumnIndexOrThrow(KEY_TREK_MIN_ALTITUDE)),
                    maxAltitude = it.getDouble(it.getColumnIndexOrThrow(KEY_TREK_MAX_ALTITUDE)),
                    waypointCount = it.getInt(it.getColumnIndexOrThrow(KEY_TREK_WAYPOINT_COUNT)),
                    notes = it.getString(it.getColumnIndexOrThrow(KEY_TREK_NOTES))
                )
            } else null
        }
    }

    fun getAllTreks(): List<Trek> {
        val trekList = mutableListOf<Trek>()
        val selectQuery = "SELECT * FROM $TABLE_TREKS ORDER BY $KEY_TREK_START_TIME DESC"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val trek = Trek(
                        id = it.getLong(it.getColumnIndexOrThrow(KEY_TREK_ID)),
                        title = it.getString(it.getColumnIndexOrThrow(KEY_TREK_TITLE)),
                        startTime = it.getLong(it.getColumnIndexOrThrow(KEY_TREK_START_TIME)),
                        endTime = it.getLong(it.getColumnIndexOrThrow(KEY_TREK_END_TIME)),
                        totalDistance = it.getDouble(it.getColumnIndexOrThrow(KEY_TREK_TOTAL_DISTANCE)),
                        totalClimb = it.getDouble(it.getColumnIndexOrThrow(KEY_TREK_TOTAL_CLIMB)),
                        totalDescent = it.getDouble(it.getColumnIndexOrThrow(KEY_TREK_TOTAL_DESCENT)),
                        minAltitude = it.getDouble(it.getColumnIndexOrThrow(KEY_TREK_MIN_ALTITUDE)),
                        maxAltitude = it.getDouble(it.getColumnIndexOrThrow(KEY_TREK_MAX_ALTITUDE)),
                        waypointCount = it.getInt(it.getColumnIndexOrThrow(KEY_TREK_WAYPOINT_COUNT)),
                        notes = it.getString(it.getColumnIndexOrThrow(KEY_TREK_NOTES))
                    )
                    trekList.add(trek)
                } while (it.moveToNext())
            }
        }
        return trekList
    }

    // --- Waypoint Operations ---

    fun insertWaypoint(waypoint: Waypoint): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_WP_TREK_ID, waypoint.trekId)
            put(KEY_WP_NAME, waypoint.name)
            put(KEY_WP_LATITUDE, waypoint.latitude)
            put(KEY_WP_LONGITUDE, waypoint.longitude)
            put(KEY_WP_ALTITUDE, waypoint.altitude)
            put(KEY_WP_TIMESTAMP, waypoint.timestamp)
            put(KEY_WP_NOTES, waypoint.notes)
        }
        return db.insert(TABLE_WAYPOINTS, null, values)
    }

    fun deleteWaypoint(waypointId: Long): Int {
        val db = this.writableDatabase
        return db.delete(TABLE_WAYPOINTS, "$KEY_WP_ID = ?", arrayOf(waypointId.toString()))
    }

    fun getWaypointsForTrek(trekId: Long): List<Waypoint> {
        val waypoints = mutableListOf<Waypoint>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_WAYPOINTS, null, "$KEY_WP_TREK_ID = ?",
            arrayOf(trekId.toString()), null, null, "$KEY_WP_TIMESTAMP ASC"
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val wp = Waypoint(
                        id = it.getLong(it.getColumnIndexOrThrow(KEY_WP_ID)),
                        trekId = it.getLong(it.getColumnIndexOrThrow(KEY_WP_TREK_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(KEY_WP_NAME)),
                        latitude = it.getDouble(it.getColumnIndexOrThrow(KEY_WP_LATITUDE)),
                        longitude = it.getDouble(it.getColumnIndexOrThrow(KEY_WP_LONGITUDE)),
                        altitude = it.getDouble(it.getColumnIndexOrThrow(KEY_WP_ALTITUDE)),
                        timestamp = it.getLong(it.getColumnIndexOrThrow(KEY_WP_TIMESTAMP)),
                        notes = it.getString(it.getColumnIndexOrThrow(KEY_WP_NOTES))
                    )
                    waypoints.add(wp)
                } while (it.moveToNext())
            }
        }
        return waypoints
    }
}
