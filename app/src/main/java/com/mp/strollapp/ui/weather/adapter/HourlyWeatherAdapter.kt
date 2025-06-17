package com.mp.strollapp.ui.weather.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mp.strollapp.R
import com.mp.strollapp.ui.weather.model.HourlyWeather
import com.mp.strollapp.ui.weather.model.getWeatherIcon

// 시간대별 날씨 정보를 표시하는 RecyclerView 어댑터
class HourlyWeatherAdapter(private val items: List<HourlyWeather>) :
    RecyclerView.Adapter<HourlyWeatherAdapter.HourlyWeatherViewHolder>() {

    class HourlyWeatherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textTime: TextView = itemView.findViewById(R.id.textTime)
        val imageWeatherIcon = itemView.findViewById<ImageView>(R.id.imageWeatherIcon)
        val textCondition: TextView = itemView.findViewById(R.id.textCondition)
        val textTemp: TextView = itemView.findViewById(R.id.textTemperature)
        val textWind = itemView.findViewById<TextView>(R.id.textWind)
        val textHumidity = itemView.findViewById<TextView>(R.id.textHumidity)

    }

    // 뷰홀더를 생성하고 레이아웃을 연결
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyWeatherViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hourly_weather, parent, false)
        return HourlyWeatherViewHolder(view)
    }
    // 각 아이템 뷰에 데이터를 바인딩
    override fun onBindViewHolder(holder: HourlyWeatherViewHolder, position: Int) {
        val item = items[position]
        holder.textTime.text = item.time // 시간 표시
        holder.imageWeatherIcon.setImageResource(getWeatherIcon(item.condition)) // 날씨 아이콘 설정
        holder.textCondition.text = item.condition // 날씨 상태
        holder.textTemp.text = item.temperature // 기온
        holder.textWind.text = item.windSpeed // 풍속
        holder.textHumidity.text = item.humidity // 습도

    }

    // 아이템 개수 반환
    override fun getItemCount(): Int = items.size
}
