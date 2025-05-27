package com.mp.strollapp.ui.walk

import android.Manifest
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
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.mp.strollapp.R
import com.mp.strollapp.data.walk.WalkRecordDatabase
import com.mp.strollapp.data.walk.WalkRecordEntity
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
        Log.d("WalkFragment", "onViewCreated called")

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
            Log.d("WalkFragment", "Start button clicked")
            startCard.visibility = View.GONE
            walkCard.visibility = View.VISIBLE
            startTracking()
        }

        stopButton.setOnClickListener {
            Log.d("WalkFragment", "Stop button clicked")
            walkCard.visibility = View.GONE
            startCard.visibility = View.VISIBLE
            stopTracking()
        }
    }

    override fun onMapReady(map: NaverMap) {
        naverMap = map

        map.locationOverlay.isVisible = true
        naverMap?.locationOverlay?.icon?.let {
        }

        map.locationTrackingMode = com.naver.maps.map.LocationTrackingMode.Follow
    }

    private fun startTracking() {
        Log.d("WalkFragment", "startTracking called")

        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("WalkFragment", "Location permission not granted")
            return
        }

        if (naverMap == null) {
            Log.e("WalkFragment", "naverMap is null")
            return
        }

        isTracking = true
        previousLocation = null
        totalDistance = 0f
        seconds = 0
        timeText.text = "00:00"
        distanceText.text = "0 m"
        pathCoords.clear()
        path.map = null // 기존 경로 제거

        // 지도 위치 초기화
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                naverMap?.moveCamera(CameraUpdate.scrollTo(latLng))
            }
        }

        // 위치 업데이트
        val request = LocationRequest.create().apply {
            interval = 3000
            fastestInterval = 2000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

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
                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.post(timerRunnable)
    }
    private fun stopTracking() {
        Log.d("WalkFragment", "stopTracking 호출")
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d("WalkFragment", "Location updates removed")
        }
        path.map = null
        timerHandler.removeCallbacks(timerRunnable)
        Log.d("WalkFragment", "Timer stopped")

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
            Log.d("WalkFragment", "산책 기록 저장 완료: $entity")
        }
    }
}
