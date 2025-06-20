package com.mp.strollapp.ui.main

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mp.strollapp.R
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.mp.strollapp.service.LocationForegroundService

class MainActivity : AppCompatActivity() {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    override fun onResume() {
        super.onResume()

    }

    override fun onStart(){
        super.onStart()
        // 위치 권한 및 GPS 확인 후 서비스 시작
        checkPermissionAndGps()
    }

    // 위치 권한 및 GPS 상태 확인
    private fun checkPermissionAndGps() {
        if (!hasLocationPermission()) {
            // 권한 없으면 요청 다이얼로그 표시
            requestLocationPermissionDialog()
            return
        }

        if (!isGpsEnabled()) {
            // GPS 꺼져 있으면 설정 유도
            showGpsEnableDialog()
            return
        }

        // 모든 조건 충족 시 하단 네비게이션 및 포그라운드 서비스 실행
        setupBottomNav()

        val serviceIntent = Intent(this, LocationForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    // 위치 권한이 있는지 확인
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // GPS 또는 네트워크 위치가 활성화되어 있는지 확인
    private fun isGpsEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkOn = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return isGpsOn || isNetworkOn
    }

    // 위치 권한 요청 다이얼로그 표시
    private fun requestLocationPermissionDialog() {
        val prefs = getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)
        val attempts = prefs.getInt("location_permission_attempts", 0)

        val builder = AlertDialog.Builder(this)
            .setTitle("위치 권한 필요")
            .setMessage("앱을 사용하려면 위치 권한이 반드시 필요합니다.")
            .setNegativeButton("앱 종료") { _, _ -> finish() }
            .setCancelable(false)

        if (attempts < 2) {
            builder.setPositiveButton("허용") { _, _ ->
                prefs.edit().putInt("location_permission_attempts", attempts + 1).apply()

                val permissions = mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
                }

                ActivityCompat.requestPermissions(
                    this,
                    permissions.toTypedArray(),
                    LOCATION_PERMISSION_REQUEST_CODE
                )

            }
        } else {
            // 두 번 이상 거부했을 경우 앱 설정으로 유도
            builder.setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }

        builder.show()
    }

    // GPS가 꺼져 있을 때 설정 유도 다이얼로그
    private fun showGpsEnableDialog() {
        AlertDialog.Builder(this)
            .setTitle("GPS가 꺼져 있습니다")
            .setMessage("정확한 위치 확인을 위해 GPS를 켜주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("앱 종료") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    // 하단 BottomNavigationView와 NavigationController 연동
    private fun setupBottomNav() {
        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        val navController = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment)
            ?.findNavController()

        navController?.let {
            navView.setupWithNavController(it)
        }
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
                resetPermissionAttempts() // 권한 허용 시 시도 횟수 초기화
                checkPermissionAndGps()
            } else {
                requestLocationPermissionDialog() // 거부 시 다시 다이얼로그 표시
            }
        }
    }

    // SharedPreferences에 저장된 권한 요청 시도 횟수 초기화
    private fun resetPermissionAttempts() {
        val prefs = getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("location_permission_attempts", 0).apply()
    }
}
