package com.manikanta.trekaltitudelogger.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.manikanta.trekaltitudelogger.R
import com.manikanta.trekaltitudelogger.data.db.TrekEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class TrekHistoryAdapter(
    private val onTrekClick: (TrekEntity) -> Unit,
    private val onTrekShare: (TrekEntity) -> Unit,
    private val onTrekDelete: (TrekEntity) -> Unit
) : RecyclerView.Adapter<TrekHistoryAdapter.TrekViewHolder>() {

    private val treks = mutableListOf<TrekEntity>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())

    fun submitList(newList: List<TrekEntity>) {
        treks.clear()
        treks.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrekViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trek, parent, false)
        return TrekViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrekViewHolder, position: Int) {
        val trek = treks[position]
        holder.bind(trek)
    }

    override fun getItemCount(): Int = treks.size

    inner class TrekViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTrekTitle)
        private val tvDate: TextView = itemView.findViewById(R.id.tvTrekDate)
        private val tvTotalClimb: TextView = itemView.findViewById(R.id.tvTotalClimb)
        private val tvTotalDistance: TextView = itemView.findViewById(R.id.tvTotalDistance)
        private val tvWaypointCount: TextView = itemView.findViewById(R.id.tvWaypointCount)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val btnExport: ImageButton = itemView.findViewById(R.id.btnExport)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteTrek)

        fun bind(trek: TrekEntity) {
            tvTitle.text = trek.name
            tvDate.text = dateFormat.format(Date(trek.startTime))
            tvTotalClimb.text = "+${trek.totalClimbMeters.roundToInt()} m"
            tvTotalDistance.text = String.format(Locale.US, "%.2f km", trek.totalDistanceKm)
            tvWaypointCount.text = "Min: ${trek.lowestPointMeters.roundToInt()}m"
            tvDuration.text = "Max: ${trek.highestPointMeters.roundToInt()}m"

            itemView.setOnClickListener { onTrekClick(trek) }
            btnExport.setOnClickListener { onTrekShare(trek) }
            btnDelete.setOnClickListener { onTrekDelete(trek) }
        }
    }
}
