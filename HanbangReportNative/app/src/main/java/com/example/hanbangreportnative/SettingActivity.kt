package com.example.hanbangreportnative

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        findViewById<BottomNavBar>(R.id.bottom_nav_bar).setCurrentScreen(3)
    }
}
