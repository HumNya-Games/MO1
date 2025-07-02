package com.example.hanbangreportnative

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.content.ContextCompat

class BottomNavBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var currentScreen: Int = 0 // 0:홈, 1:리포트, 2:음성, 3:설정

    init {
        inflate(context, R.layout.bottom_nav_bar, this)
        orientation = HORIZONTAL
        setupNavButtons()
    }

    fun setCurrentScreen(index: Int) {
        currentScreen = index
        updateIcons()
    }

    private fun setupNavButtons() {
        val navButtons = listOf(
            Triple(findViewById<ImageButton>(R.id.nav_home), R.drawable.icon_home, R.drawable.icon_home_active),
            Triple(findViewById<ImageButton>(R.id.nav_report), R.drawable.icon_report, R.drawable.icon_report_active),
            Triple(findViewById<ImageButton>(R.id.nav_speech), R.drawable.icon_speech, R.drawable.icon_speech_active),
            Triple(findViewById<ImageButton>(R.id.nav_setting), R.drawable.icon_setting, R.drawable.icon_setting_active)
        )
        navButtons.forEachIndexed { index, triple ->
            val (button, normalRes, activeRes) = triple
            button.setOnClickListener {
                if (currentScreen == index) return@setOnClickListener
                val activity = context as? android.app.Activity
                when (index) {
                    0 -> {
                        context.startActivity(Intent(context, MainActivity::class.java))
                        activity?.overridePendingTransition(0, 0)
                    }
                    1 -> {
                        context.startActivity(Intent(context, ReportListActivity::class.java))
                        activity?.overridePendingTransition(0, 0)
                    }
                    2 -> {
                        context.startActivity(Intent(context, SpeechSettingActivity::class.java))
                        activity?.overridePendingTransition(0, 0)
                    }
                    3 -> {
                        context.startActivity(Intent(context, SettingActivity::class.java))
                        activity?.overridePendingTransition(0, 0)
                    }
                }
            }
        }
        updateIcons()
    }

    private fun updateIcons() {
        val navButtons = listOf(
            Triple(findViewById<ImageButton>(R.id.nav_home), R.drawable.icon_home, R.drawable.icon_home_active),
            Triple(findViewById<ImageButton>(R.id.nav_report), R.drawable.icon_report, R.drawable.icon_report_active),
            Triple(findViewById<ImageButton>(R.id.nav_speech), R.drawable.icon_speech, R.drawable.icon_speech_active),
            Triple(findViewById<ImageButton>(R.id.nav_setting), R.drawable.icon_setting, R.drawable.icon_setting_active)
        )
        navButtons.forEachIndexed { index, triple ->
            val (button, normalRes, activeRes) = triple
            button.setImageResource(if (index == currentScreen) activeRes else normalRes)
        }
    }
}
