package com.mp.strollapp.ui.weather.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mp.strollapp.R
import com.mp.strollapp.ui.weather.model.HourlyWeather

class HourlyWeatherAdapter(private val items: List<HourlyWeather>) :
    RecyclerView.Adapter<HourlyWeatherAdapter.HourlyWeatherViewHolder>() {

    class HourlyWeatherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textTime: TextView = itemView.findViewById(R.id.textTime)
        val textCondition: TextView = itemView.findViewById(R.id.textCondition)
        val textTemp: TextView = itemView.findViewById(R.id.textTemperature)
        val textWind = itemView.findViewById<TextView>(R.id.textWind)
        val textHumidity = itemView.findViewById<TextView>(R.id.textHumidity)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyWeatherViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hourly_weather, parent, false)
        return HourlyWeatherViewHolder(view)
    }

    override fun onBindViewHolder(holder: HourlyWeatherViewHolder, position: Int) {
        val item = items[position]
        holder.textTime.text = item.time
        holder.textCondition.text = item.condition
        holder.textTemp.text = item.temperature
        holder.textWind.text = item.windSpeed
        holder.textHumidity.text = item.humidity
    }

    override fun getItemCount(): Int = items.size
}
