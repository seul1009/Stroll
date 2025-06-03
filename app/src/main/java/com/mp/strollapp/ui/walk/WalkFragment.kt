package com.mp.strollapp.ui.walk

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.mp.strollapp.R
import com.mp.strollapp.data.walk.WalkRecordDatabase
import com.mp.strollapp.data.walk.WalkRecordEntity
import com.mp.strollapp.service.LocationForegroundService
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.PathOverlay
import kotlinx.coroutines.launch

class WalkFragment : Fragment(), OnMapReadyCallback {

    private lateinit var startCard: LinearLayout
    private lateinit var walkCard: LinearLayout
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var timeText: TextView
    private lateinit var distanceText: TextView

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var previousLocation: Location? = null
    private var totalDistance = 0f
    private var isTracking = false

    private var seconds = 0
    private lateinit var timerHandler: Handler
    private lateinit var timerRunnable: Runnable

    private var path = PathOverlay()
    private val pathCoords = mutableListOf<LatLng>()
    private var naverMap: NaverMap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_walk, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startCard = view.findViewById(R.id.startCard)
        walkCard = view.findViewById(R.id.walkCard)
        startButton = view.findViewById(R.id.startButton)
        stopButton = view.findViewById(R.id.stopButton)
        timeText = view.findViewById(R.id.timeText)
        distanceText = view.findViewById(R.id.distanceText)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        timerHandler = Handler(Looper.getMainLooper())

        val fm = childFragmentManager
        var mapFragment = fm.findFragmentById(R.id.map_fragment) as? MapFragment
        if (mapFragment == null) {
            Log.d("WalkFragment", "mapFragment is null, creating new one")
            mapFragment = MapFragment.newInstance()
            fm.beginTransaction().replace(R.id.map_fragment, mapFragment).commitNow()
        }
        mapFragment?.getMapAsync(this)

        startButton.setOnClickListener {
            startCard.visibility = View.GONE
            walkCard.visibility = View.VISIBLE
            startTracking()

            val intent = Intent(requireContext(), LocationForegroundService::class.java)
            requireContext().startService(intent)

            // 산책 중 상태 저장
            val prefs = requireContext().getSharedPreferences("walk_state", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("isWalking", true)
                .putBoolean("hasStarted", true)
                .apply()

            // 위치 이동
            if (ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val latLng = LatLng(it.latitude, it.longitude)
                        naverMap?.moveCamera(CameraUpdate.scrollTo(latLng))
                        naverMap?.locationOverlay?.position = latLng
                    }
                }
            }
        }

        stopButton.setOnClickListener {
            if (!isTracking) {
                Toast.makeText(requireContext(), "산책이 진행 중이 아닙니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            walkCard.visibility = View.GONE
            startCard.visibility = View.VISIBLE
            stopTracking()

            val intent = Intent(requireContext(), LocationForegroundService::class.java)
            requireContext().stopService(intent)

            // 산책 중 상태 제거
            val prefs = requireContext().getSharedPreferences("walk_state", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("isWalking", false)
                .putBoolean("hasStarted", false)
                .apply()

            Toast.makeText(requireContext(), "산책이 기록되었어요!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startTracking(resume: Boolean = false) {
        if (isTracking) return
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        if (naverMap == null) {
            Log.e("WalkFragment", "naverMap is null")
            return
        }

        if (resume) {
            val prefs = requireContext().getSharedPreferences("walk_state", Context.MODE_PRIVATE)
            totalDistance = prefs.getFloat("prev_distance", 0f)
            seconds = prefs.getInt("prev_seconds", 0)
            timeText.text = String.format("%02d:%02d", seconds / 60, seconds % 60)
            distanceText.text = "${totalDistance.toInt()} m"

            val pathString = prefs.getString("prev_path", "") ?: ""
            pathCoords.clear()
            pathString.split("|").forEach {
                val parts = it.split(",")
                if (parts.size == 2) {
                    val lat = parts[0].toDoubleOrNull()
                    val lng = parts[1].toDoubleOrNull()
                    if (lat != null && lng != null) {
                        pathCoords.add(LatLng(lat, lng))
                    }
                }
            }

            if (pathCoords.size >= 2) {
                path.coords = pathCoords
                path.map = naverMap
                previousLocation = Location("").apply {
                    latitude = pathCoords.last().latitude
                    longitude = pathCoords.last().longitude
                }
            }
        } else {
            totalDistance = 0f
            seconds = 0
            pathCoords.clear()
            path.map = null
            timeText.text = "00:00"
            distanceText.text = "0 m"
            previousLocation = null
        }

        // 지도 위치 초기화
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                naverMap?.moveCamera(CameraUpdate.scrollTo(latLng))
            }
        }

        // 위치 요청 생성
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(2000L)
            .build()

        // 위치 콜백
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    Log.d("WalkFragment", "Location received: ${location.latitude}, ${location.longitude}")
                    val latLng = LatLng(location.latitude, location.longitude)
                    naverMap?.locationOverlay?.position = latLng

                    previousLocation?.let {
                        val distance = it.distanceTo(location)
                        if (distance > 2f) {
                            totalDistance += distance
                            distanceText.text = "${totalDistance.toInt()} m"
                        }
                    }

                    pathCoords.add(latLng)

                    if (pathCoords.size >= 2) {
                        path.coords = pathCoords
                        if (path.map == null) {
                            Log.d("WalkFragment", "Path has enough points, setting map")
                            path.map = naverMap
                        }
                    }

                    naverMap?.moveCamera(CameraUpdate.scrollTo(latLng))
                    previousLocation = location
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())

        timerRunnable = object : Runnable {
            override fun run() {
                seconds++
                val minutes = seconds / 60
                val sec = seconds % 60
                timeText.text = String.format("%02d:%02d", minutes, sec)

                context?.getSharedPreferences("walk_state", Context.MODE_PRIVATE)?.edit()?.apply {
                    putInt("prev_seconds", seconds)
                    putFloat("prev_distance", totalDistance)
                    putString("prev_path", pathCoords.joinToString("|") { "${it.latitude},${it.longitude}" })
                    apply()
                }

                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.post(timerRunnable)

        isTracking = true
    }


    private fun stopTracking() {
        isTracking = false

        val prefs = requireContext().getSharedPreferences("walk_state", Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("prev_distance", totalDistance)
            .putInt("prev_seconds", seconds)
            .putString("prev_path", pathCoords.joinToString("|") { "${it.latitude},${it.longitude}" })
            .apply()

        // 위치 업데이트 중지
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d("WalkFragment", "Location updates removed")
        }

        path.map = null
        timerHandler.removeCallbacks(timerRunnable)

        val db = WalkRecordDatabase.getInstance(requireContext())
        val pathString = pathCoords.joinToString(";") { "${it.latitude},${it.longitude}" }

        lifecycleScope.launch {
            val entity = WalkRecordEntity(
                distance = totalDistance.toDouble(),
                duration = seconds,
                timestamp = System.currentTimeMillis(),
                path = pathString
            )
            db.walkRecordDao().insert(entity)
        }

    }

    private var shouldResumeTracking = false

    override fun onResume() {
        super.onResume()

        val prefs = requireContext().getSharedPreferences("walk_state", Context.MODE_PRIVATE)
        val isWalking = prefs.getBoolean("isWalking", false)
        val hasStarted = prefs.getBoolean("hasStarted", false)

        if (isWalking && hasStarted && !isTracking) {
            startCard.visibility = View.GONE
            walkCard.visibility = View.VISIBLE

            if (naverMap != null) {
                startTracking(resume = true)
            } else {
                shouldResumeTracking = true
            }
        } else {
            startCard.visibility = View.VISIBLE
            walkCard.visibility = View.GONE
        }
    }

    override fun onMapReady(map: NaverMap) {
        naverMap = map
        map.locationOverlay.isVisible = true
        map.locationTrackingMode = com.naver.maps.map.LocationTrackingMode.Follow

        if (shouldResumeTracking && !isTracking) {
            startTracking(resume = true)
            shouldResumeTracking = false
        }
    }

    override fun onPause() {
        super.onPause()
        if (isTracking.not()) {
            if (::locationCallback.isInitialized) {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
            timerHandler.removeCallbacks(timerRunnable)
        }
    }

}
