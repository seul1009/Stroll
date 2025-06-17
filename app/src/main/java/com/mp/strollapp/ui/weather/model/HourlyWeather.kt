package com.mp.strollapp.ui.weather.model

import com.mp.strollapp.R

// 시간대별 날씨 정보를 담는 데이터 클래스
data class HourlyWeather(
    val time: String, // 시간
    val condition: String, // 날씨 상태
    val temperature: String, // 온도
    val windSpeed: String, // 풍속
    val humidity: String, // 습도
    val sky: String, // 하늘 상태 코드
    val pty: String, // 강수 형태 코드
    val rain: String // 강수량
)

// 날씨 상태 문자열에 따라 아이콘 리소스를 반환
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
