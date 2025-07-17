package com.example.hanbangreportnative

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.LinearLayout
import android.util.AttributeSet
import android.view.View
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Button
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import android.os.Build
import android.widget.Toast
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.util.Log
import android.view.ViewGroup

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavBar: BottomNavBar
    private val REQUEST_MIC_PERMISSION = 3001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // LogManager 초기화
        LogManager.init(this)

        // 운행 시작 버튼 클릭 리스너
        findViewById<ImageView>(R.id.driving_circle).setOnClickListener {
            showDrivingPopup()
        }

        // 각 신고 리스트 버튼 클릭 리스너
        findViewById<LinearLayout>(R.id.in_progress_button).setOnClickListener {
            navigateToReportList("in_progress")
        }
        findViewById<LinearLayout>(R.id.completed_button).setOnClickListener {
            navigateToReportList("completed")
        }
        findViewById<LinearLayout>(R.id.rejected_button).setOnClickListener {
            navigateToReportList("rejected")
        }

        // Bottom Navigation Bar 초기화
        findViewById<BottomNavBar>(R.id.bottom_nav_bar).setCurrentScreen(0)
    }

    override fun onResume() {
        super.onResume()
        updateReportCounts()
        // 앱이 포그라운드에 있을 때 플로팅 볼 서비스 중지
        stopFloatingBallService()
    }

    override fun onPause() {
        super.onPause()
        // 앱이 백그라운드로 갈 때는 플로팅 볼 서비스를 중지하지 않음
        // (운행 시작 버튼을 눌러야만 플로팅 볼이 시작됨)
    }

    // 플로팅 볼 서비스 중지
    private fun stopFloatingBallService() {
        try {
            val intent = Intent(this, FloatingBallService::class.java)
            stopService(intent)
            Log.d("MainActivity", "[상태체크] 앱 포그라운드 진입 - 플로팅 볼 서비스 중지")
        } catch (e: Exception) {
            Log.e("MainActivity", "[상태체크] 플로팅 볼 서비스 중지 실패: ${e.message}")
        }
    }

    private fun updateReportCounts() {
        val reportList = ReportDataStore.loadList(this)
        val deletedCount = ReportDataStore.getDeletedCount(this)

        // N1: 신고 대기 개수
        val waitingCount = reportList.count { it.type == "신고 대기" }
        findViewById<TextView>(R.id.tv_in_progress_count)?.text = "${waitingCount}건"

        // N2: 신고 완료 개수
        val completedCount = reportList.count { it.type == "신고 완료" }
        findViewById<TextView>(R.id.tv_completed_count)?.text = "${completedCount}건"

        // N3: 삭제된 신고 완료 개수
        findViewById<TextView>(R.id.tv_rejected_count)?.text = "${deletedCount}건"

        // N2+N3: 현재 신고 현황
        val totalCount = completedCount + deletedCount
        findViewById<TextView>(R.id.tv_total_count)?.text = "${totalCount}건"
    }

    private fun navigateToReportList(status: String) {
        // 리포트 리스트 화면으로 이동하는 로직
        val intent = Intent(this, ReportListActivity::class.java).apply {
            putExtra("REPORT_STATUS", status) // 상태 정보 전달
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun navigateTo(activityClass: Class<*>, status: String? = null) {
        val intent = Intent(this, activityClass).apply {
            status?.let { putExtra("REPORT_STATUS", it) }
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun showDrivingPopup() {
        val prefs = getSharedPreferences("drive_prefs", Context.MODE_PRIVATE)
        val dontShowAgain = prefs.getBoolean("dont_show_again", false)
        if (dontShowAgain) {
            startFloatingBallService()
            moveTaskToBack(true)
            return
        }
        val dialog = object : Dialog(this) {
            var checked = false
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                val view = LayoutInflater.from(context).inflate(R.layout.dialog_drive_start, null)
                setContentView(view)
                // 검은색 알파 배경 적용 및 전체 화면 덮기
                window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#80000000")))
                window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setCancelable(true) // 바깥 터치 및 뒤로가기 허용
                setCanceledOnTouchOutside(true) // 바깥 터치 시 닫힘 유지
                
                // 배경 터치 시 팝업 닫기
                view.setOnTouchListener { _, event ->
                    if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                        dismiss()
                        true
                    } else {
                        false
                    }
                }
                
                val checkIcon = view.findViewById<ImageView>(R.id.iv_check)
                val checkText = view.findViewById<TextView>(R.id.tv_check_text)
                val btnConfirm = view.findViewById<Button>(R.id.btn_confirm)
                btnConfirm.backgroundTintList = null
                fun updateCheckUI() {
                    if (checked) {
                        checkIcon.setImageResource(R.drawable.ic_check_on)
                        checkIcon.setBackgroundResource(R.drawable.check_bg_on)
                        checkText.setTextColor(Color.parseColor("#1ED760"))
                    } else {
                        checkIcon.setImageResource(R.drawable.ic_check_off)
                        checkIcon.setBackgroundResource(R.drawable.check_bg_off)
                        checkText.setTextColor(Color.parseColor("#000000"))
                    }
                }
                updateCheckUI()
                checkIcon.setOnClickListener {
                    checked = !checked
                    updateCheckUI()
                }
                checkText.setOnClickListener {
                    checked = !checked
                    updateCheckUI()
                }
                btnConfirm.setOnClickListener {
                    if (checked) {
                        prefs.edit().putBoolean("dont_show_again", true).apply()
                    }
                    startFloatingBallService()
                    moveTaskToBack(true)
                    dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun startFloatingBallService() {
        Log.d("MainActivity", "startFloatingBallService() 호출")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.e("MainActivity", "오버레이 권한 없음")
            ToastUtils.showCustomToast(this, "플로팅 볼 오버레이 권한이 필요합니다. 설정에서 권한을 허용해 주세요.")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.e("MainActivity", "노티피케이션 권한 없음")
            ToastUtils.showCustomToast(this, "플로팅 볼 알림 권한이 필요합니다. 설정에서 권한을 허용해 주세요.")
            return
        }
        // 마이크 권한 체크 및 요청
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_MIC_PERMISSION)
            return
        }
        val intent = Intent(this, FloatingBallService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("MainActivity", "startForegroundService 호출")
            startForegroundService(intent)
        } else {
            Log.d("MainActivity", "startService 호출")
            startService(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MIC_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 승인 시 플로팅 볼 서비스 시작
                startFloatingBallService()
            } else {
                ToastUtils.showCustomToast(this, "마이크 권한이 없으면 음성인식 기능이 제한됩니다.")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 앱 종료 시 플로팅 볼 서비스 중지
        stopFloatingBallService()
        // 앱 완전 종료
        android.os.Process.killProcess(android.os.Process.myPid())
        System.exit(0)
    }
}
