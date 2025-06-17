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

    // 날씨 데이터 리스트를 저장할 LiveData
    private val _weatherList = MutableLiveData<List<HourlyWeather>>()
    val weatherList: LiveData<List<HourlyWeather>> get() = _weatherList

    // 사용자 위치 주소를 저장할 LiveData
    private val _address = MutableLiveData<String>()
    val address: LiveData<String> get() = _address

    // 주소 값을 설정하는 함수
    fun setAddress(newAddress: String) {
        _address.postValue(newAddress)
    }

    // 날씨 API를 호출하고 결과를 파싱하여 weatherList에 저장
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
                // 응답이 성공이면 파싱하여 리스트로 변환
                if (response.isSuccessful) {
                    parseResponseToWeatherList(response.body()?.response?.body?.items?.item)
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "API 호출 실패: ${e.message}")
                emptyList()
            }
            // 결과를 LiveData에 반영
            _weatherList.postValue(result)
        }
    }

    // API 응답 데이터를 시간별 날씨 정보 리스트로 변환
    private fun parseResponseToWeatherList(items: List<WeatherItem>?): List<HourlyWeather> {
        if (items == null) return emptyList()
        // fcstTime(예보 시간) 기준으로 그룹화
        val grouped = items.groupBy { it.fcstTime }

        // 각 시간별로 필요한 항목 추출 후 HourlyWeather 객체로 매핑
        return grouped.mapNotNull { (time, list) ->
            val tmp = list.find { it.category == "TMP" }?.fcstValue // 기온
            val sky = list.find { it.category == "SKY" }?.fcstValue // 하늘 상태
            val pty = list.find { it.category == "PTY" }?.fcstValue // 강수 형태
            val wsd = list.find { it.category == "WSD" }?.fcstValue // 풍속
            val reh = list.find { it.category == "REH" }?.fcstValue // 습도
            val rain = list.find { it.category == "POP"}?.fcstValue // 강수 확률

            if (tmp != null && sky != null && pty != null && wsd != null && reh != null && rain != null) {
                HourlyWeather(
                    time = "${time.substring(0, 2)}:${time.substring(2)}",
                    condition = getWeatherCondition(sky, pty),
                    temperature = "$tmp°C",
                    windSpeed = "$wsd m/s",
                    humidity = "$reh%",
                    sky = sky,
                    pty = pty,
                    rain = rain

                )
            } else null
        }
    }

    // SKY, PTY 값을 기반으로 날씨 상태 텍스트 반환
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
