package com.mp.strollapp.ui.weather

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mp.strollapp.BuildConfig
import com.mp.strollapp.R
import com.mp.strollapp.data.weather.GpsUtil
import com.mp.strollapp.data.weather.WeatherAPI
import com.mp.strollapp.ui.weather.adapter.HourlyWeatherAdapter
import com.mp.strollapp.ui.weather.model.HourlyWeather
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.cancellation.CancellationException

class WeatherDetailActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_detail)

        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        startLocationAndWeatherProcess()
    }

    private fun startLocationAndWeatherProcess() {
        if (!isGpsEnabled()) {
            showGpsDialog()
            return
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerHourlyWeather)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        lifecycleScope.launch {
            try {
                if (!hasLocationPermission()) {
                    Log.e("WeatherDetail", "위치 권한이 없음. 위치 요청 중단")
                    return@launch
                }

                val location = try {
                    if (ActivityCompat.checkSelfPermission(
                            this@WeatherDetailActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        throw SecurityException("위치 권한 없음")
                    }
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY, null
                    ).await()
                } catch (ce: CancellationException) {
                    Log.e("WeatherDetail", "Coroutine이 취소됨: ${ce.message}")
                    null
                } catch (se: SecurityException) {
                    Log.e("WeatherDetail", "보안 예외 발생: ${se.message}")
                    null
                } ?: try {
                    if (ActivityCompat.checkSelfPermission(
                            this@WeatherDetailActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        throw SecurityException("위치 권한 없음")
                    }
                    fusedLocationClient.lastLocation.await()
                } catch (e: Exception) {
                    Log.e("WeatherDetail", "lastLocation 실패: ${e.message}")
                    null
                }

                if (location != null) {
                    val grid = GpsUtil.convertGRID_GPS(location.latitude, location.longitude)
                    val nx = grid["nx"] ?: 55
                    val ny = grid["ny"] ?: 127

                    try {
                        val textLocation = findViewById<TextView>(R.id.textLocation)
                        val geocoder = Geocoder(this@WeatherDetailActivity, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val address = addresses?.firstOrNull()?.getAddressLine(0) ?: "위치 알 수 없음"
                        textLocation.text = address

                    } catch (e: Exception) {
                        Log.e("WeatherDetail", "주소 변환 실패: ${e.message}")
                    }

                    val weatherList = fetchWeatherList(nx, ny)
                    recyclerView.adapter = HourlyWeatherAdapter(weatherList)


                    val todayWeather = weatherList.firstOrNull()
                    if (todayWeather != null) {
                        val textWindSpeed = findViewById<TextView>(R.id.textWindSpeed)
                        val textHumidity = findViewById<TextView>(R.id.textHumidity)
                        val textAverageTemp = findViewById<TextView>(R.id.textAverageTemp)

                        textWindSpeed.text = todayWeather.windSpeed
                        textHumidity.text = todayWeather.humidity
                        textAverageTemp.text = todayWeather.temperature
                    }
                }
            } catch (e: Exception) {
                Log.e("WeatherDetail", "위치 정보 불러오기 실패: ${e.message}")
            }

        }
    }

    private suspend fun fetchWeatherList(nx: Int, ny: Int): List<HourlyWeather> {
        return try {
            val response = WeatherAPI.api.getForecast(
                serviceKey = BuildConfig.WEATHER_API_KEY,
                numOfRows = 1000,
                pageNo = 1,
                dataType = "JSON",
                baseDate = getTodayDate(),
                baseTime = getLatestBaseTime(),
                nx = nx,
                ny = ny
            )

            if (response.isSuccessful) {
                val items = response.body()?.response?.body?.items?.item ?: return emptyList()
                val grouped = items.groupBy { it.fcstTime }

                grouped.mapNotNull { (time, list) ->
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
                            humidity = "$reh%"
                        )
                    } else null
                }
            } else {
                Log.e("WeatherDetail", "날씨 정보 불러오기 실패: ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("WeatherDetail", "API 호출 실패: ${e.message}")
            emptyList()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationAndWeatherProcess()
            } else {
                Log.e("WeatherDetail", "위치 권한 거부됨")
            }
        }
    }

    private fun isGpsEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun showGpsDialog() {
        AlertDialog.Builder(this@WeatherDetailActivity)
            .setTitle("GPS 꺼짐")
            .setMessage("정확한 날씨 정보 제공을 위해 위치(GPS)를 켜주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("취소", null)
            .show()
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