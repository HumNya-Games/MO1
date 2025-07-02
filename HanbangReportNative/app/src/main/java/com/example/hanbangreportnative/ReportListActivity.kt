package com.example.hanbangreportnative

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout
import android.view.View

class ReportListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_list)
        findViewById<BottomNavBar>(R.id.bottom_nav_bar).setCurrentScreen(1)
    }
}
