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
import androidx.lifecycle.ViewModelProvider
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class WeatherDetailActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var viewModel: WeatherViewModel
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_detail)

        viewModel = ViewModelProvider(this)[WeatherViewModel::class.java]
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        setupRecyclerView()
        observeViewModel()
        requestLocationAndWeather()
    }

    private fun setupRecyclerView() {
        findViewById<RecyclerView>(R.id.recyclerHourlyWeather).layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun observeViewModel() {
        viewModel.weatherList.observe(this) { weatherList ->
            findViewById<RecyclerView>(R.id.recyclerHourlyWeather).adapter =
                HourlyWeatherAdapter(weatherList)

            weatherList.firstOrNull()?.let { today ->
                findViewById<TextView>(R.id.textWindSpeed).text = today.windSpeed
                findViewById<TextView>(R.id.textHumidity).text = today.humidity
                findViewById<TextView>(R.id.textAverageTemp).text = today.temperature

                val weatherText = when (today.pty) {
                    "1" -> "현재 비가 와요"
                    "2" -> "현재 비/눈이 와요 "
                    "3" -> "지금 눈이 오고 있어요 !"
                    "4" -> "현재 소나기가 내려요"
                    else -> when (today.sky) {
                        "1" -> "날씨가 좋네요! \n 나가서 산책 어때요?"
                        "3" -> "구름이 많네요 \n 그래도 나가서 산책 어때요?"
                        "4" -> "지금은 흐리네요"
                        else -> "날씨 정보 없음"
                    }
                }
                findViewById<TextView>(R.id.textWeatherComment).text = weatherText
            }
        }

        viewModel.address.observe(this) {
            findViewById<TextView>(R.id.textLocation).text = it
        }
    }

    private fun requestLocationAndWeather() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGpsDialog()
            return
        }

        lifecycleScope.launch {
            if (!hasLocationPermission()) {
                Log.e("WeatherDetail", "위치 권한이 없음. 위치 요청 중단")
                return@launch
            }

            val location = withContext(Dispatchers.IO) {
                try {
                    if (ActivityCompat.checkSelfPermission(
                            this@WeatherDetailActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        throw SecurityException("위치 권한 없음")
                    }
                    fusedLocationClient.lastLocation.await()
                        ?: fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY, null
                        ).await()
                } catch (e: Exception) {
                    Log.e("WeatherDetail", "위치 요청 실패: ${e.message}")
                    null
                }
            }

            if (location != null) {
                val grid = GpsUtil.convertGRID_GPS(location.latitude, location.longitude)
                val nx = grid["nx"] ?: 55
                val ny = grid["ny"] ?: 127

                val geocoder = Geocoder(this@WeatherDetailActivity, Locale.getDefault())
                val address = try {
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        ?.firstOrNull()?.getAddressLine(0)
                } catch (e: Exception) {
                    Log.e("WeatherDetail", "주소 변환 실패: ${e.message}")
                    null
                } ?: "위치 알 수 없음"

                viewModel.setAddress(address)
                viewModel.fetchWeatherList(nx, ny, getTodayDate(), getLatestBaseTime())
            } else {
                Log.e("WeatherDetail", "위치 정보가 null입니다.")
            }
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
                requestLocationAndWeather()
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

}
