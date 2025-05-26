package com.mp.strollapp.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mp.strollapp.BuildConfig
import com.mp.strollapp.data.model.WeatherItem
import com.mp.strollapp.data.weather.WeatherService
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainViewModel : ViewModel() {

    private val _temperature = MutableLiveData<String?>()
    val temperature: LiveData<String?> get() = _temperature

    private val _weatherCondition = MutableLiveData<String?>()
    val weatherCondition: LiveData<String?> get() = _weatherCondition

    private val _address = MutableLiveData<String>()
    val address: LiveData<String> get() = _address

    fun setAddress(newAddress: String) {
        _address.value = newAddress
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(WeatherService::class.java)

    fun fetchWeather(nx: Int, ny: Int, baseDate: String, baseTime: String) {
        viewModelScope.launch {
            try {
                val response = service.getForecast(
                    serviceKey = BuildConfig.WEATHER_API_KEY,
                    numOfRows = 100,
                    pageNo = 1,
                    dataType = "JSON",
                    baseDate = baseDate,
                    baseTime = baseTime,
                    nx = nx,
                    ny = ny
                )

                if (response.isSuccessful) {
                    val items = response.body()?.response?.body?.items?.item

                    val tmp = items?.find { it.category == "TMP" }?.fcstValue
                    val sky = items?.find { it.category == "SKY" }?.fcstValue
                    val pty = items?.find { it.category == "PTY" }?.fcstValue

                    Log.d("WeatherAPI", "기온: $tmp, SKY: $sky, PTY: $pty")

                    _temperature.postValue(tmp?.let { "$it°C" })
                    _weatherCondition.postValue(getWeatherCondition(sky, pty))
                } else {
                    Log.e("WeatherAPI", "응답 실패: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("WeatherAPI", "API 요청 중 오류: ${e.message}")
            }
        }
    }

    private fun getWeatherCondition(sky: String?, pty: String?): String {
        return when (pty) {
            "1" -> "비"
            "2" -> "비/눈"
            "3" -> "눈"
            "4" -> "소나기"
            "0", null -> when (sky) {
                "1" -> "맑음"
                "3" -> "구름많음"
                "4" -> "약간흐림"
                else -> "알 수 없음"
            }
            else -> "알 수 없음"
        }
    }
}
