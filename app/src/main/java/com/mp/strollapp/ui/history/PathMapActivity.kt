package com.mp.strollapp.ui.history

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mp.strollapp.R
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.overlay.PathOverlay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.MarkerIcons

class PathMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_path_map)

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        // 네이버 지도를 비동기로 초기화하고 경로 및 마커 표시
        mapView.getMapAsync { naverMap ->
            // 인텐트에서 path 데이터 추출
            val pathString = intent.getStringExtra("path") ?: return@getMapAsync
            val coords = parsePathString(pathString) // String → List<LatLng> 변환

            // 지도 위에 경로 표시
            val pathOverlay = PathOverlay().apply {
                this.coords = coords
                color = Color.YELLOW
                width = 10
            }
            pathOverlay.map = naverMap

            if (coords.isNotEmpty()) {
                // 첫 위치로 카메라 이동
                val cameraUpdate = CameraUpdate.scrollTo(coords[0])
                naverMap.moveCamera(cameraUpdate)

                // 출발점 마커
                val startMarker = Marker().apply {
                    position = coords.first()
                    captionText = "출발"
                    icon = MarkerIcons.BLACK
                    iconTintColor = Color.parseColor("#01e432")
                }
                startMarker.map = naverMap

                // 도착점 마커
                val endMarker = Marker().apply {
                    position = coords.last()
                    captionText = "도착"
                    icon = MarkerIcons.BLACK
                    iconTintColor = Color.parseColor("#fe3939")
                }
                endMarker.map = naverMap
            }
        }

        // UI 텍스트뷰 초기화 및 데이터 세팅
        val textDate = findViewById<TextView>(R.id.textDate)
        val textDistance = findViewById<TextView>(R.id.textDistance)
        val textDuration = findViewById<TextView>(R.id.textDuration)

        // timestamp 포맷팅
        val timestamp = intent.getLongExtra("timestamp", 0L)
        val date = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault()).format(Date(timestamp))
        textDate.text = "날짜: $date"

        // 거리 km 변환
        val distance = intent.getDoubleExtra("distance", 0.0) / 1000.0
        textDistance.text = "거리: %.2f km".format(distance)

        // 시간 시:분:초 변환
        val duration = intent.getIntExtra("duration", 0)
        val formattedDuration = formatDuration(duration)
        textDuration.text = "시간: $formattedDuration"

    }

    // 산책 시간(초)을 "hh:mm:ss" 포맷으로 변환
    private fun formatDuration(durationSec: Int): String {
        val h = durationSec / 3600
        val m = (durationSec % 3600) / 60
        val s = durationSec % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    // 경로 문자열을 List<LatLng>으로 파싱
    private fun parsePathString(pathString: String): List<LatLng> {
        return pathString.split(";").mapNotNull {
            val parts = it.split(",")
            if (parts.size == 2) {
                val lat = parts[0].toDoubleOrNull()
                val lng = parts[1].toDoubleOrNull()
                if (lat != null && lng != null) LatLng(lat, lng) else null
            } else null
        }
    }

    // MapView 생명주기 동기화
    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onDestroy() { super.onDestroy(); mapView.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
}
