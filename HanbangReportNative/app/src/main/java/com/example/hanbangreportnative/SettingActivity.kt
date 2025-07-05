package com.example.hanbangreportnative

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.content.Context
import android.widget.Toast

class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        findViewById<BottomNavBar>(R.id.bottom_nav_bar).setCurrentScreen(3)
        findViewById<Button>(R.id.btn_reset_data).setOnClickListener {
            // 신고 데이터 초기화 (기존 구조)
            val prefs = getSharedPreferences("report_data", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            
            // 새로운 신고 리스트 데이터 초기화
            ReportDataStore.clearAll(this)
            ReportDataStore.resetDeletedCount(this)
            
            // 다시 보지 않기 상태 초기화
            val drivePrefs = getSharedPreferences("drive_prefs", Context.MODE_PRIVATE)
            drivePrefs.edit().clear().apply()
            Toast.makeText(this, "데이터가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
