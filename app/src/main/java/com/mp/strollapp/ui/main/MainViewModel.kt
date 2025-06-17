package com.mp.strollapp.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mp.strollapp.BuildConfig
import com.mp.strollapp.data.weather.WeatherService
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mp.strollapp.data.walk.WalkRecordDatabase
import com.mp.strollapp.data.walk.WalkRecordEntity

class MainViewModel(application: Application) : AndroidViewModel(application) {
    // 현재 기온
    private val _temperature = MutableLiveData<String?>()
    val temperature: LiveData<String?> get() = _temperature

    // 현재 날씨 상태
    private val _weatherCondition = MutableLiveData<String?>()
    val weatherCondition: LiveData<String?> get() = _weatherCondition

    // 현재 주소
    private val _address = MutableLiveData<String>()
    val address: LiveData<String> get() = _address

    fun setAddress(newAddress: String) {
        _address.value = newAddress
    }

    // Room DB의 DAO 객체
    private val walkRecordDao = WalkRecordDatabase.getInstance(application).walkRecordDao()

    // 오늘의 누적 산책 거리 및 시간
    private val _todayWalkSummary = MutableLiveData<Pair<Int, Int>>()
    val todayWalkSummary: LiveData<Pair<Int, Int>> get() = _todayWalkSummary

    // 오늘 날짜의 모든 산책 기록을 불러와 거리/시간 합산
    fun fetchTodayWalkSummary() {
        viewModelScope.launch {
            val records: List<WalkRecordEntity> = walkRecordDao.getTodayRecords()
            val totalDistance = records.sumOf { it.distance }.toInt()
            val totalDuration = records.sumOf { it.duration }

            _todayWalkSummary.postValue(Pair(totalDistance, totalDuration))
        }
    }

    // 날씨 API를 위한 Retrofit 인스턴스 생성
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(WeatherService::class.java)

    // 기상청 날씨 정보 조회 및 LiveData 업데이트
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

                    // TMP: 기온, SKY: 하늘 상태, PTY: 강수 형태
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

    // SKY, PTY 코드 값을 기반으로 날씨 상태 텍스트 반환
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
