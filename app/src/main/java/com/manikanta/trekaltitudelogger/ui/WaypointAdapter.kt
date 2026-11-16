package com.manikanta.trekaltitudelogger.ui

import android.graphics.Color
import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.manikanta.trekaltitudelogger.R
import com.manikanta.trekaltitudelogger.data.UnitFormatter
import com.manikanta.trekaltitudelogger.data.db.WaypointEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class WaypointAdapter(
    private val onWaypointDelete: (WaypointEntity) -> Unit
) : RecyclerView.Adapter<WaypointAdapter.WaypointViewHolder>() {

    private val waypoints = mutableListOf<WaypointEntity>()
    private val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())

    fun submitList(newList: List<WaypointEntity>) {
        waypoints.clear()
        waypoints.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WaypointViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_waypoint, parent, false)
        return WaypointViewHolder(view)
    }

    override fun onBindViewHolder(holder: WaypointViewHolder, position: Int) {
        val waypoint = waypoints[position]
        val prevWaypoint = if (position > 0) waypoints[position - 1] else null
        holder.bind(waypoint, prevWaypoint, position + 1)
    }

    override fun getItemCount(): Int = waypoints.size

    inner class WaypointViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBadge: TextView = itemView.findViewById(R.id.tvWaypointBadge)
        private val tvName: TextView = itemView.findViewById(R.id.tvWaypointName)
        private val tvTime: TextView = itemView.findViewById(R.id.tvWaypointTime)
        private val tvAltitude: TextView = itemView.findViewById(R.id.tvAltitude)
        private val tvElevationDelta: TextView = itemView.findViewById(R.id.tvElevationDelta)
        private val tvLegDistance: TextView = itemView.findViewById(R.id.tvLegDistance)
        private val tvNotes: TextView = itemView.findViewById(R.id.tvNotes)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(waypoint: WaypointEntity, prevWaypoint: WaypointEntity?, index: Int) {
            tvBadge.text = index.toString()
            tvName.text = waypoint.name
            val timeStr = timeFormat.format(Date(waypoint.timestamp))
            val coordsStr = UnitFormatter.formatCoordinates(waypoint.latitude, waypoint.longitude)
            tvTime.text = "$timeStr • $coordsStr"

            tvAltitude.text = String.format(Locale.US, "%.1f m", waypoint.altitude)

            if (prevWaypoint != null) {
                val delta = waypoint.altitude - prevWaypoint.altitude
                if (delta > 0) {
                    tvElevationDelta.text = "↑ +${delta.roundToInt()} m"
                    tvElevationDelta.setTextColor(Color.parseColor("#2E7D32")) // Green
                } else if (delta < 0) {
                    tvElevationDelta.text = "↓ -${abs(delta).roundToInt()} m"
                    tvElevationDelta.setTextColor(Color.parseColor("#C62828")) // Red
                } else {
                    tvElevationDelta.text = "→ 0 m"
                    tvElevationDelta.setTextColor(Color.GRAY)
                }

                val results = FloatArray(1)
                Location.distanceBetween(
                    prevWaypoint.latitude, prevWaypoint.longitude,
                    waypoint.latitude, waypoint.longitude,
                    results
                )
                tvLegDistance.text = "Leg: ${UnitFormatter.formatDistance(results[0].toDouble())}"
                tvLegDistance.visibility = View.VISIBLE
            } else {
                tvElevationDelta.text = "Start Point"
                tvElevationDelta.setTextColor(Color.GRAY)
                tvLegDistance.text = "Leg: 0 m"
            }

            tvNotes.visibility = View.GONE

            btnDelete.setOnClickListener {
                onWaypointDelete(waypoint)
            }
        }
    }
}
