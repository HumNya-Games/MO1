package com.example.hanbangreportnative

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.LinearLayout
import android.util.AttributeSet
import android.view.View

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
        // 단속 주행 안내 팝업 구현
    }
}
