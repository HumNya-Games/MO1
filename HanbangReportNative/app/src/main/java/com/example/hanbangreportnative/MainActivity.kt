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

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavBar: BottomNavBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                // 검은색 알파 배경 적용
                window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#80000000")))
                setCancelable(true) // 바깥 터치 및 뒤로가기 허용
                val checkIcon = view.findViewById<ImageView>(R.id.iv_check)
                val checkText = view.findViewById<TextView>(R.id.tv_check_text)
                val btnConfirm = view.findViewById<Button>(R.id.btn_confirm)
                // 하단 녹색 안내 버튼 동적 추가
                val greenBtn = Button(context).apply {
                    text = "다음부터 안내 보지 않기"
                    setTextColor(Color.parseColor("#1ED760"))
                    textSize = 16f
                    setBackgroundResource(R.drawable.check_bg_on)
                    visibility = View.GONE
                }
                val parent = view as? android.widget.LinearLayout
                parent?.addView(greenBtn)
                fun updateCheckUI() {
                    if (checked) {
                        checkIcon.setImageResource(R.drawable.ic_check_on)
                        checkIcon.setBackgroundResource(R.drawable.check_bg_on)
                        checkText.setTextColor(Color.parseColor("#1ED760"))
                        greenBtn.visibility = View.VISIBLE
                    } else {
                        checkIcon.setImageResource(R.drawable.ic_check_off)
                        checkIcon.setBackgroundResource(R.drawable.check_bg_off)
                        checkText.setTextColor(Color.parseColor("#CCCCCC"))
                        greenBtn.visibility = View.GONE
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
            Toast.makeText(this, "플로팅 볼 오버레이 권한이 필요합니다. 설정에서 권한을 허용해 주세요.", Toast.LENGTH_LONG).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.e("MainActivity", "노티피케이션 권한 없음")
            Toast.makeText(this, "플로팅 볼 알림 권한이 필요합니다. 설정에서 권한을 허용해 주세요.", Toast.LENGTH_LONG).show()
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
}
