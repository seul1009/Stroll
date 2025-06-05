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

        // 비동기로 네이버 지도 객체를 받아 지도 설정
        mapView.getMapAsync { naverMap ->
            val pathString = intent.getStringExtra("path") ?: return@getMapAsync
            val coords = parsePathString(pathString)

            // 지도 위에 경로 표시
            val pathOverlay = PathOverlay().apply {
                this.coords = coords
                color = Color.YELLOW
                width = 10
            }
            pathOverlay.map = naverMap

            if (coords.isNotEmpty()) {
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

        val textDate = findViewById<TextView>(R.id.textDate)
        val textDistance = findViewById<TextView>(R.id.textDistance)
        val textDuration = findViewById<TextView>(R.id.textDuration)

        val timestamp = intent.getLongExtra("timestamp", 0L)
        val date = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault()).format(Date(timestamp))
        textDate.text = "날짜: $date"

        val distance = intent.getDoubleExtra("distance", 0.0) / 1000.0
        textDistance.text = "거리: %.2f km".format(distance)

        val duration = intent.getIntExtra("duration", 0)
        val formattedDuration = formatDuration(duration)
        textDuration.text = "시간: $formattedDuration"

    }

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

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onDestroy() { super.onDestroy(); mapView.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
}
