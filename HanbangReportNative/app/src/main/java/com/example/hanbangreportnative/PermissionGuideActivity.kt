package com.example.hanbangreportnative

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AlertDialog
import android.Manifest
import androidx.core.content.ContextCompat

class PermissionGuideActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_guide)

        val infoText = findViewById<TextView>(R.id.permission_info_text)
        val requestBtn = findViewById<Button>(R.id.permission_request_btn)

        // 오버레이 권한 체크
        if (!Settings.canDrawOverlays(this)) {
            infoText.text = "플로팅 볼 기능을 위해 오버레이 권한이 필요합니다.\n권한을 허용해 주세요."
            requestBtn.setOnClickListener {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivityForResult(intent, 1001)
            }
            return
        }
        // 마이크 권한 체크 (위치 권한 등보다 먼저)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            showMicPermissionDialog()
            return
        }
        // 노티피케이션 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            showNotificationPermissionDialog()
            return
        }
        // 위치 권한 체크 (Android 14 이상은 FOREGROUND_SERVICE_LOCATION도 필요)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            (Build.VERSION.SDK_INT >= 34 && ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            showLocationPermissionDialog()
            return
        }
        // Android 11 이상: 백그라운드 위치 권한 별도 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            showBackgroundLocationPermissionDialog()
            return
        }
        // 모든 권한이 있으면 메인으로 이동
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    // 마이크 권한 안내 다이얼로그
    private fun showMicPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("마이크 권한 필요")
            .setMessage("음성인식 기능을 위해 마이크 권한이 필요합니다.\n권한을 허용해 주세요.")
            .setCancelable(false)
            .setPositiveButton("권한 허용") { _, _ ->
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 2001)
            }
            .setNegativeButton("나중에") { _, _ ->
                ToastUtils.showCustomToast(this, "마이크 권한이 없으면 음성인식 기능이 제한됩니다.")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .show()
    }

    private fun showNotificationPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("알림 권한 필요")
            .setMessage("플로팅 볼 알림을 위해 알림 권한이 필요합니다.\n권한을 허용해 주세요.")
            .setCancelable(false)
            .setPositiveButton("권한 허용") { _, _ ->
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1002)
            }
            .setNegativeButton("나중에") { _, _ ->
                ToastUtils.showCustomToast(this, "알림 권한이 없으면 일부 기능이 제한될 수 있습니다.")
                // 알림 권한 없이도 위치 권한 안내로 진행
                showLocationPermissionDialog()
            }
            .show()
    }

    private fun showLocationPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("위치 권한 필요")
            .setMessage("플로팅 볼의 위치 기능을 위해 위치 권한이 필요합니다.\n권한을 허용해 주세요.")
            .setCancelable(false)
            .setPositiveButton("권한 허용") { _, _ ->
                val permissions = mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                if (Build.VERSION.SDK_INT >= 34) {
                    permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
                }
                ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1003)
            }
            .setNegativeButton("나중에") { _, _ ->
                ToastUtils.showCustomToast(this, "위치 권한이 없으면 일부 기능이 제한될 수 있습니다.")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .show()
    }

    private fun showBackgroundLocationPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("백그라운드 위치 권한 필요")
            .setMessage("플로팅 볼이 백그라운드에서도 위치를 저장하려면 '항상 허용' 권한이 필요합니다.\n권한을 허용해 주세요.")
            .setCancelable(false)
            .setPositiveButton("권한 허용") { _, _ ->
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 1010)
            }
            .setNegativeButton("나중에") { _, _ ->
                ToastUtils.showCustomToast(this, "백그라운드 위치 권한이 없으면 일부 기능이 제한될 수 있습니다.")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            if (Settings.canDrawOverlays(this)) {
                // 오버레이 권한 허용 후 마이크 권한 체크 및 안내
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    showMicPermissionDialog()
                    return
                }
                // 오버레이 권한 허용 후 노티피케이션 권한 안내
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    showNotificationPermissionDialog()
                } else {
                    showLocationPermissionDialog()
                }
            } else {
                ToastUtils.showCustomToast(this, "오버레이 권한이 필요합니다.")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 2001) {
            // 마이크 권한 요청 결과
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ToastUtils.showCustomToast(this, "마이크 권한이 허용되었습니다.")
                // 다음 권한 안내로 진행
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    showNotificationPermissionDialog()
                } else {
                    showLocationPermissionDialog()
                }
            } else {
                ToastUtils.showCustomToast(this, "마이크 권한이 필요합니다.")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            return
        }
        if (requestCode == 1002) {
            // 노티피케이션 권한 요청 결과 후 위치 권한 안내
            showLocationPermissionDialog()
        } else if (requestCode == 1003) {
            // 위치 권한 요청 결과 후 백그라운드 권한 체크
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    showBackgroundLocationPermissionDialog()
                    return
                }
                ToastUtils.showCustomToast(this, "위치 권한이 허용되었습니다.")
            } else {
                ToastUtils.showCustomToast(this, "위치 권한이 필요합니다.")
            }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else if (requestCode == 1010) {
            // 백그라운드 위치 권한 요청 결과
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ToastUtils.showCustomToast(this, "백그라운드 위치 권한이 허용되었습니다.")
            } else {
                ToastUtils.showCustomToast(this, "백그라운드 위치 권한이 필요합니다.")
            }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    // 권한 목록에 마이크 권한 추가
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.RECORD_AUDIO // 마이크 권한 추가
    )

    private fun checkPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 100)
        } else {
            // 모든 권한이 허용됨
            finish()
        }
    }

    // 권한 안내 메시지에도 마이크 권한 안내 추가
    private fun getPermissionGuideText(): String {
        return "앱의 원활한 사용을 위해 위치, 오버레이, 마이크 권한이 필요합니다.\n설정에서 권한을 허용해 주세요."
    }
} 