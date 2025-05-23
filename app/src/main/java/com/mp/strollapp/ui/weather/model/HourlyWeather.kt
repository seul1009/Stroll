package com.mp.strollapp.ui.weather.model

data class HourlyWeather(
    val time: String,
    val condition: String,
    val temperature: String,
    val windSpeed: String,
    val humidity: String
)
