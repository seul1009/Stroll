package com.mp.strollapp.ui.weather

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mp.strollapp.BuildConfig
import com.mp.strollapp.data.model.WeatherItem
import com.mp.strollapp.data.weather.WeatherAPI
import com.mp.strollapp.ui.weather.model.HourlyWeather
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WeatherViewModel : ViewModel() {

    private val _weatherList = MutableLiveData<List<HourlyWeather>>()
    val weatherList: LiveData<List<HourlyWeather>> get() = _weatherList

    private val _address = MutableLiveData<String>()
    val address: LiveData<String> get() = _address

    fun setAddress(newAddress: String) {
        _address.postValue(newAddress)
    }

    fun fetchWeatherList(nx: Int, ny: Int, baseDate: String, baseTime: String) {
        viewModelScope.launch {
            val result = try {
                val response = WeatherAPI.api.getForecast(
                    serviceKey = BuildConfig.WEATHER_API_KEY,
                    numOfRows = 1000,
                    pageNo = 1,
                    dataType = "JSON",
                    baseDate = baseDate,
                    baseTime = baseTime,
                    nx = nx,
                    ny = ny
                )
                if (response.isSuccessful) {
                    parseResponseToWeatherList(response.body()?.response?.body?.items?.item)
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "API 호출 실패: ${e.message}")
                emptyList()
            }
            _weatherList.postValue(result)
        }
    }

    private fun parseResponseToWeatherList(items: List<WeatherItem>?): List<HourlyWeather> {
        if (items == null) return emptyList()
        val grouped = items.groupBy { it.fcstTime }

        return grouped.mapNotNull { (time, list) ->
            val tmp = list.find { it.category == "TMP" }?.fcstValue
            val sky = list.find { it.category == "SKY" }?.fcstValue
            val pty = list.find { it.category == "PTY" }?.fcstValue
            val wsd = list.find { it.category == "WSD" }?.fcstValue
            val reh = list.find { it.category == "REH" }?.fcstValue

            if (tmp != null && sky != null && pty != null && wsd != null && reh != null) {
                HourlyWeather(
                    time = "${time.substring(0, 2)}:${time.substring(2)}",
                    condition = getWeatherCondition(sky, pty),
                    temperature = "$tmp°C",
                    windSpeed = "$wsd m/s",
                    humidity = "$reh%",
                    sky = sky,
                    pty = pty
                )
            } else null
        }
    }

    private fun getTodayDate(): String =
        SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(Date())

    private fun getLatestBaseTime(): String {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)
        val current = hour * 100 + minute
        val times = listOf("0200", "0500", "0800", "1100", "1400", "1700", "2000", "2300")
        return times.lastOrNull { current >= it.toInt() } ?: "0200"
    }

    private fun getWeatherCondition(sky: String, pty: String): String {
        return when (pty) {
            "1" -> "비"
            "2" -> "비/눈"
            "3" -> "눈"
            "4" -> "소나기"
            else -> when (sky) {
                "1" -> "맑음"
                "3" -> "구름많음"
                "4" -> "흐림"
                else -> "알 수 없음"
            }
        }
    }
}
