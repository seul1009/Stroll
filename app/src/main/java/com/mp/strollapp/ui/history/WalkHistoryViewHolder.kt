package com.mp.strollapp.ui.history

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mp.strollapp.R
import com.mp.strollapp.data.walk.WalkRecordEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// RecyclerView의 각 아이템을 관리하는 ViewHolder
class WalkHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    // 항목 내 UI 구성 요소 연결
    private val textDistance: TextView = itemView.findViewById(R.id.textDistance)
    private val textDuration: TextView = itemView.findViewById(R.id.textDuration)
    private val textDate: TextView = itemView.findViewById(R.id.textDate)

    // ViewHolder에 데이터 바인딩
    fun bind(record: WalkRecordEntity) {
        textDistance.text = "거리: %.2f km".format(record.distance / 1000.0)

        // 소요 시간 포맷팅 후 표시
        val durationFormatted = formatDuration(record.duration)
        textDuration.text = "시간: $durationFormatted"

        // 타임스탬프 → "yyyy.MM.dd HH:mm" 형식으로 날짜 포맷팅
        val dateFormatted = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
            .format(Date(record.timestamp))
        textDate.text = dateFormatted
    }

    // 초 단위 시간 → "hh:mm:ss" 문자열로 변환
    private fun formatDuration(durationSec: Int): String {
        val h = durationSec / 3600
        val m = (durationSec % 3600) / 60
        val s = durationSec % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
}
