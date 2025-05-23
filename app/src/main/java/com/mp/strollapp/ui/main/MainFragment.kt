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
import android.util.Log
import java.util.Calendar

class MainFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var lastLat: Double? = null
    private var lastLon: Double? = null
    private var stayStartTime: Long? = null
    private lateinit var textStayTime: TextView


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
            textTemperature.text = temp
        }

        viewModel.weatherCondition.observe(viewLifecycleOwner) { condition ->
            // SKY: 1=맑음, 3=구름많음, 4=흐림
            // PTY: 0=없음, 1=비, 2=비/눈, 3=눈, 4=소나기
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

        layoutWeather.setOnClickListener {
            val intent = android.content.Intent(
                requireContext(),
                com.mp.strollapp.ui.weather.WeatherDetailActivity::class.java
            )
            startActivity(intent)
        }

        getCurrentLocationAndFetchWeather()
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

        val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            if (location == null) {
                Log.e("MainFragment", "getCurrentLocation가 null입니다.")
                return@addOnSuccessListener
            }

            val lat = location.latitude
            val lon = location.longitude
            Log.d("GPS", "실제 위도/경도: lat=$lat, lon=$lon")
            val grid = GpsUtil.convertGRID_GPS(lat, lon)
            Log.d("GPS", "변환된 격자: nx=${grid["nx"]}, ny=${grid["ny"]}")

            val nx = grid["nx"] ?: 60
            val ny = grid["ny"] ?: 127

            val baseDate = SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(Date())
            val baseTime = getLatestBaseTime()

            viewModel.fetchWeather(nx, ny, baseDate, baseTime)
        }.addOnFailureListener {
            Log.e("MainFragment", "현재 위치 요청 실패: ${it.message}")

        }
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

