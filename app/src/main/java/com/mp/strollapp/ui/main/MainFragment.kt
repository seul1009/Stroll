package com.mp.strollapp.ui.main

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mp.strollapp.R
import com.mp.strollapp.data.weather.GpsUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mp.strollapp.ui.weather.WeatherDetailActivity
import java.util.Calendar

class MainFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var lastLat: Double? = null
    private var lastLon: Double? = null
    private var stayStartTime: Long? = null
    private lateinit var textStayTime: TextView

    private val _temperature = MutableLiveData<String?>()
    val temperature: LiveData<String?> get() = _temperature

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val textTemperature = view.findViewById<TextView>(R.id.textTemperature)
        val imageWeatherIcon = view.findViewById<ImageView>(R.id.imageWeatherIcon)
        val layoutWeather = view.findViewById<View>(R.id.layoutWeather)
        textStayTime = view.findViewById(R.id.textStayTime)

        viewModel.temperature.observe(viewLifecycleOwner) { temp ->
            textTemperature.text = temp ?: "--°C"
        }

        viewModel.weatherCondition.observe(viewLifecycleOwner) { condition ->
            Log.d("날씨 조건", "condition = $condition")
            imageWeatherIcon.setImageResource(
                when (condition) {
                    "맑음" -> R.drawable.ic_sunny
                    "구름많음" -> R.drawable.ic_cloudy
                    "약간흐림" -> R.drawable.ic_sunny_cloudy
                    "비" -> R.drawable.ic_rainy
                    "눈" -> R.drawable.ic_snow
                    else -> R.drawable.ic_weather_unknown
                }
            )
        }

        getCurrentLocationAndFetchWeather()

        layoutWeather.setOnClickListener {
            val intent = Intent(requireContext(), WeatherDetailActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getCurrentLocationAndFetchWeather() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1000
            )
            return
        }

        val locationManager = context?.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            Log.e("MainFragment", "GPS/Network 위치 모두 꺼져 있음 - 설정에서 켜주세요")
            return
        }

        val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            if (location == null) {
                Log.e("MainFragment", "실시간 위치 요청 실패 (null 반환)")
                // 사용자에게 안내
                showLocationFailedDialog()
                return@addOnSuccessListener
            }

            // 위치 정상 수신 시
            val lat = location.latitude
            val lon = location.longitude
            val grid = GpsUtil.convertGRID_GPS(lat, lon)
            val nx = grid["nx"] ?: 60
            val ny = grid["ny"] ?: 127
            val baseDate = SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(Date())
            val baseTime = getLatestBaseTime()
            viewModel.fetchWeather(nx, ny, baseDate, baseTime)
        }.addOnFailureListener {
            Log.e("MainFragment", "실시간 위치 요청 중 오류: ${it.message}")
            showLocationFailedDialog()
        }
    }

    private fun showLocationFailedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("위치 수신 실패")
            .setMessage("현재 위치 정보를 받아올 수 없습니다.\nWi-Fi 또는 GPS 신호가 약할 수 있습니다.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("닫기", null)
            .show()
    }

    private fun getLatestBaseTime(): String {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)

        val timeList = listOf("0200", "0500", "0800", "1100", "1400", "1700", "2000", "2300")
        val current = hour * 100 + minute
        var result = "0200"

        for (t in timeList) {
            if (current >= t.toInt()) result = t
        }

        return result
    }
}

