package com.example.hanbangreportnative

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

class BottomNavBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var currentScreen: Int = 0 // 0:홈, 1:리포트, 2:음성, 3:설정

    // 아이콘 크기 배율 변수 (외부에서 조절 가능)
    var activeIconScale: Float = 1.5f
    var inactiveIconScale: Float = 0.9f
    private val baseIconWidth = 33    // dp, 기본 아이콘 width
    private val baseIconHeight = 50   // dp, 기본 아이콘 height

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
        val navLabels = listOf(
            findViewById<TextView>(R.id.nav_home_label),
            findViewById<TextView>(R.id.nav_report_label),
            findViewById<TextView>(R.id.nav_speech_label),
            findViewById<TextView>(R.id.nav_setting_label)
        )
        navButtons.forEachIndexed { index, triple ->
            val (button, normalRes, activeRes) = triple
            val isActive = index == currentScreen
            button.setImageResource(if (isActive) activeRes else normalRes)
            // 크기 조절
            val scale = if (isActive) activeIconScale else inactiveIconScale
            val widthPx = (baseIconWidth * scale * resources.displayMetrics.density).toInt()
            val heightPx = (baseIconHeight * scale * resources.displayMetrics.density).toInt()
            val params = button.layoutParams
            params.width = widthPx
            params.height = heightPx
            button.layoutParams = params
            // 안내 텍스트 표시/숨김
            navLabels[index].visibility = if (isActive) View.GONE else View.VISIBLE
        }
    }
}
