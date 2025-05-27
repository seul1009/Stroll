package com.mp.strollapp.ui.history

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mp.strollapp.R
import com.mp.strollapp.data.walk.WalkRecordEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WalkHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val textDistance: TextView = itemView.findViewById(R.id.textDistance)
    private val textDuration: TextView = itemView.findViewById(R.id.textDuration)
    private val textDate: TextView = itemView.findViewById(R.id.textDate)

    fun bind(record: WalkRecordEntity) {
        textDistance.text = "거리: %.2f km".format(record.distance)

        val durationFormatted = formatDuration(record.duration)
        textDuration.text = "시간: $durationFormatted"

        val dateFormatted = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
            .format(Date(record.timestamp))
        textDate.text = dateFormatted
    }

    private fun formatDuration(durationSec: Int): String {
        val h = durationSec / 3600
        val m = (durationSec % 3600) / 60
        val s = durationSec % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
}
