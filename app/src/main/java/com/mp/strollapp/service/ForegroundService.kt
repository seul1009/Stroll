package com.mp.strollapp.service
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.mp.strollapp.R
import kotlinx.coroutines.launch

class LocationForegroundService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var previousLocation: android.location.Location? = null
    private var totalDistance = 0f
    private var seconds = 0
    private val handler = android.os.Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel(this)

        // FusedLocationProviderClient 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 위치 업데이트 요청 간격 설정
        locationRequest = LocationRequest.create().apply {
            interval = 60_000L // 1분마다 요청
            fastestInterval = 30_000L // 최소 30초 간격
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY // 고정밀도 모드
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return

                // 이전 위치와 비교하여 거리 계산
                previousLocation?.let {
                    val distance = it.distanceTo(location) // 거리 계산 (단위: m)
                    if (distance > 2f) { // 2m 이상 이동 시만 반영
                        totalDistance += distance

                        // 거리 SharedPreferences 저장
                        val prefs = getSharedPreferences("walk_state", Context.MODE_PRIVATE)
                        prefs.edit().putFloat("prev_distance", totalDistance).apply()
                    }
                }
                previousLocation = location

                Log.d("ForegroundService", "거리: ${totalDistance.toInt()} m, 시간: ${seconds}s")
            }
        }

        // 1초마다 시간 증가 및 SharedPreferences에 저장
        runnable = object : Runnable {
            override fun run() {
                seconds++

                // 시간 SharedPreferences 저장
                val prefs = getSharedPreferences("walk_state", Context.MODE_PRIVATE)
                prefs.edit().putInt("prev_seconds", seconds).apply()

                handler.postDelayed(this, 1000) // 1초마다 반복
            }
        }
        handler.post(runnable)

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 포그라운드 서비스 시작 및 위치 추적 시작
        startForegroundAndLocationUpdates()
        return START_STICKY
    }

    // 포그라운드 알림 생성 + 위치 추적 시작
    private fun startForegroundAndLocationUpdates() {
        val channelId = "location_channel"
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Stroll 위치 감지 중")
            .setContentText("한 장소에 머무는 시간을 추적하고 있어요.")
            .setSmallIcon(R.drawable.ic_walk)
            .build()

        startForeground(1, notification)

        // 위치 권한이 있는 경우에만 위치 업데이트 요청
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    // 알림 채널 생성
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "location_channel",
                "위치 추적 서비스",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // 서비스 종료 시 위치 업데이트와 시간 추적 중단
    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        handler.removeCallbacks(runnable)


    }
}
