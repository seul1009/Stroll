package com.mp.strollapp.ui.weather.model

import com.mp.strollapp.R

data class HourlyWeather(
    val time: String,
    val condition: String,
    val temperature: String,
    val windSpeed: String,
    val humidity: String,
)

fun getWeatherIcon(condition: String): Int {
    return when (condition) {
        "맑음" -> R.drawable.ic_sunny
        "구름많음" -> R.drawable.ic_sunny_cloudy
        "흐림" -> R.drawable.ic_cloudy
        "비" -> R.drawable.ic_rainy
        "소나기" -> R.drawable.ic_rainy
        "눈" -> R.drawable.ic_snow
        else -> R.drawable.ic_weather_unknown
    }
}
