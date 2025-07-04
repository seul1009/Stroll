package com.mp.strollapp.ui.weather

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
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

        // ViewModel 및 위치 클라이언트 초기화
        viewModel = ViewModelProvider(this)[WeatherViewModel::class.java]
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 위치 권한 확인
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        setupRecyclerView()
        observeViewModel()
        requestLocationAndWeather()
    }

    // 시간별 날씨 리사이클러뷰 설정
    private fun setupRecyclerView() {
        findViewById<RecyclerView>(R.id.recyclerHourlyWeather).layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    // ViewModel의 LiveData 관찰
    private fun observeViewModel() {
        val recycler = findViewById<RecyclerView>(R.id.recyclerHourlyWeather)
        val loadingText = findViewById<TextView>(R.id.textHourlyLoading)
        val commentText = findViewById<TextView>(R.id.textWeatherComment)

        // 시간별 날씨 리스트 관찰
        viewModel.weatherList.observe(this) { weatherList ->
            if (weatherList.isNullOrEmpty()) {
                // 로딩 중
                recycler.visibility = View.GONE
                loadingText.visibility = View.VISIBLE
                commentText.text = "" // 또는 commentText.visibility = View.GONE
            } else {
                // 로딩 완료
                recycler.adapter = HourlyWeatherAdapter(weatherList)
                recycler.visibility = View.VISIBLE
                loadingText.visibility = View.GONE
                commentText.visibility = View.VISIBLE

                // 가장 첫 번째 시간의 날씨 정보로 상세 데이터 표시
                weatherList.firstOrNull()?.let { today ->
                    findViewById<TextView>(R.id.textWindSpeed).text = today.windSpeed
                    findViewById<TextView>(R.id.textHumidity).text = today.humidity
                    findViewById<TextView>(R.id.textRain).text = "${today.rain}%"
                    findViewById<TextView>(R.id.textAverageTemp).text = today.temperature

                    // 날씨 조건에 따른 메시지 설정
                    val weatherText = when (today.pty) {
                        "1" -> "현재 비가 와요"
                        "2" -> "현재 비/눈이 와요 "
                        "3" -> "지금 눈이 오고 있어요 !"
                        "4" -> "현재 소나기가 내려요"
                        else -> when (today.sky) {
                            "1" -> "날씨가 좋네요! \n 나가서 산책 어때요?"
                            "3" -> "구름이 많아요 \n 그래도 산책하기 좋아요 !"
                            "4" -> "지금은 흐리네요"
                            else -> "날씨 정보 없음"
                        }
                    }
                    commentText.text = weatherText

                    // 날씨 조건에 따라 아이콘 설정
                    val imageWeather = findViewById<ImageView>(R.id.imageWeather)
                    val iconRes = when (today.pty) {
                        "1" -> R.drawable.ic_rainy
                        "2" -> R.drawable.ic_snow
                        "3" -> R.drawable.ic_snow
                        "4" -> R.drawable.ic_rainy
                        else -> when (today.sky) {
                            "1" -> R.drawable.ic_sunny
                            "3" -> R.drawable.ic_cloudy
                            "4" -> R.drawable.ic_sunny_cloudy
                            else -> R.drawable.ic_weather_unknown
                        }
                    }
                    imageWeather.setImageResource(iconRes)
                }
            }
        }
        // 주소 텍스트 업데이트
        viewModel.address.observe(this) {
            findViewById<TextView>(R.id.textLocation).text = it
        }
    }

    // 위치 요청 및 날씨 정보 API 호출
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

            // 위치 가져오기
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

            // 위치 기반 날씨 API 요청
            if (location != null) {
                val grid = GpsUtil.convertGRID_GPS(location.latitude, location.longitude)
                val nx = grid["nx"] ?: 55
                val ny = grid["ny"] ?: 127

                val geocoder = Geocoder(this@WeatherDetailActivity, Locale.KOREAN)
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

    // 위치 권한 확인
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 위치 권한 요청
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    // 권한 요청 결과 처리
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

    // GPS 꺼져 있을 경우 설정창 유도 다이얼로그
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

    // 오늘 날짜를 yyyyMMdd 형식으로 반환
    private fun getTodayDate(): String =
        SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(Date())

    // 기상청 API 호출을 위한 가장 가까운 base time 계산
    private fun getLatestBaseTime(): String {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)
        val current = hour * 100 + minute
        val times = listOf("0200", "0500", "0800", "1100", "1400", "1700", "2000", "2300")
        return times.lastOrNull { current >= it.toInt() } ?: "0200"
    }

}
