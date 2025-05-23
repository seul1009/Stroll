package com.mp.strollapp.ui.walk

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.mp.strollapp.R
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.PathOverlay

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
            mapFragment = MapFragment.newInstance()
            fm.beginTransaction().replace(R.id.map_fragment, mapFragment).commitNow()
        }
        mapFragment?.getMapAsync(this)

        startButton.setOnClickListener {
            startCard.visibility = View.GONE
            walkCard.visibility = View.VISIBLE
            startTracking()
        }

        stopButton.setOnClickListener {
            walkCard.visibility = View.GONE
            startCard.visibility = View.VISIBLE
            stopTracking()
        }
    }

    override fun onMapReady(map: NaverMap) {
        naverMap = map
    }

    private fun startTracking() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        isTracking = true
        previousLocation = null
        totalDistance = 0f
        seconds = 0
        pathCoords.clear()
        path.map = naverMap

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    val latLng = LatLng(location.latitude, location.longitude)

                    // 거리 계산
                    previousLocation?.let {
                        val distance = it.distanceTo(location)
                        totalDistance += distance
                        distanceText.text = "${totalDistance.toInt()} m"
                    }

                    pathCoords.add(latLng)

                    // 경로가 2개 이상일 때만 지도에 등록
                    if (pathCoords.size >= 2) {
                        path.coords = pathCoords
                        if (path.map == null) path.map = naverMap
                    }

                    // 지도 위치 이동
                    naverMap?.moveCamera(com.naver.maps.map.CameraUpdate.scrollTo(latLng))

                    previousLocation = location
                }
            }
        }

        val request = LocationRequest.create().apply {
            interval = 3000
            fastestInterval = 2000
            priority = Priority.PRIORITY_HIGH_ACCURACY
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
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        path.map = null
        timerHandler.removeCallbacks(timerRunnable)
    }
}
