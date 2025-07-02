package com.example.hanbangreportnative

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout
import android.util.AttributeSet
import android.view.View

class SpeechSettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_setting)
        findViewById<BottomNavBar>(R.id.bottom_nav_bar).setCurrentScreen(2)
    }
}
