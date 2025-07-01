package com.example.hanbangreportnative

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 네비게이션 아이콘 크기 배율 변수
        val activeScale = 1.2f // 활성화 시 아이콘 크기 배율 (수정 가능)
        val inactiveScale = 1.0f

        // 네비게이션 아이콘 버튼 리스트
        val navButtons = listOf(
            Triple(findViewById<ImageButton>(R.id.nav_home), "icon_home", "icon_home_active"),
            Triple(findViewById<ImageButton>(R.id.nav_report), "icon_report", "icon_report_active"),
            Triple(findViewById<ImageButton>(R.id.nav_speech), "icon_speech", "icon_speech_active"),
            Triple(findViewById<ImageButton>(R.id.nav_setting), "icon_setting", "icon_setting_active")
        )

        val currentScreen = 0 // 홈 화면이므로 0 (필요시 변경)

        navButtons.forEachIndexed { index, triple ->
            val (button, normalResName, activeResName) = triple
            val isActive = index == currentScreen
            val resId = resources.getIdentifier(
                if (isActive) activeResName else normalResName,
                "drawable",
                packageName
            )
            button.setImageResource(resId)
            button.scaleX = if (isActive) activeScale else inactiveScale
            button.scaleY = if (isActive) activeScale else inactiveScale
        }

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
    }

    private fun showDrivingPopup() {
        // 단속 주행 안내 팝업 구현
    }

    private fun navigateToReportList(status: String) {
        // 리포트 리스트 화면으로 이동하는 로직
    }
}
