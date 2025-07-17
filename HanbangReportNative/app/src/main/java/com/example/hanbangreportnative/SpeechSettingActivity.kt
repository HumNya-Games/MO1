package com.example.hanbangreportnative

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout
import android.util.AttributeSet
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.content.SharedPreferences

class SpeechSettingActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var advancedSettingsLayout: LinearLayout
    private lateinit var advancedSettingsButton: TextView
    private var isAdvancedSettingsVisible = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_setting)
        findViewById<BottomNavBar>(R.id.bottom_nav_bar).setCurrentScreen(2)

        // LogManager 초기화
        LogManager.init(this)
        
        prefs = getSharedPreferences("speech_settings", MODE_PRIVATE)
        
        // 고급 설정 UI 요소들
        advancedSettingsLayout = findViewById(R.id.layout_advanced_settings)
        advancedSettingsButton = findViewById(R.id.btn_advanced_settings)
        
        // 고급 설정 토글 버튼
        advancedSettingsButton.setOnClickListener {
            toggleAdvancedSettings()
        }
        
        // 마이크 민감도 설정
        val seekBar = findViewById<SeekBar>(R.id.seekbar_mic_sensitivity)
        val valueText = findViewById<TextView>(R.id.text_mic_sensitivity_value)
        val saved = prefs.getInt("mic_sensitivity", 2000)
        seekBar.progress = saved
        valueText.text = saved.toString()
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                valueText.text = progress.toString()
                prefs.edit().putInt("mic_sensitivity", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 높은 볼륨 임계값 설정
        val highThresholdSeekBar = findViewById<SeekBar>(R.id.seekbar_high_sensitivity_threshold)
        val highThresholdValueText = findViewById<TextView>(R.id.text_high_sensitivity_threshold_value)
        val savedHighThreshold = prefs.getInt("high_sensitivity_threshold", 3000)
        highThresholdSeekBar.progress = savedHighThreshold
        highThresholdValueText.text = savedHighThreshold.toString()
        highThresholdSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                highThresholdValueText.text = progress.toString()
                prefs.edit().putInt("high_sensitivity_threshold", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 중간 볼륨 임계값 설정
        val mediumThresholdSeekBar = findViewById<SeekBar>(R.id.seekbar_medium_sensitivity_threshold)
        val mediumThresholdValueText = findViewById<TextView>(R.id.text_medium_sensitivity_threshold_value)
        val savedMediumThreshold = prefs.getInt("medium_sensitivity_threshold", 1500)
        mediumThresholdSeekBar.progress = savedMediumThreshold
        mediumThresholdValueText.text = savedMediumThreshold.toString()
        mediumThresholdSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mediumThresholdValueText.text = progress.toString()
                prefs.edit().putInt("medium_sensitivity_threshold", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 로그 간소화 설정
        setupLogSimplificationSettings()
    }
    
    private fun toggleAdvancedSettings() {
        isAdvancedSettingsVisible = !isAdvancedSettingsVisible
        
        if (isAdvancedSettingsVisible) {
            advancedSettingsLayout.visibility = View.VISIBLE
            advancedSettingsButton.text = "고급 설정 숨기기"
        } else {
            advancedSettingsLayout.visibility = View.GONE
            advancedSettingsButton.text = "고급 설정 보기"
        }
    }
    
    private fun setupLogSimplificationSettings() {
        // 로그 간소화 토글 버튼 찾기 (레이아웃에 추가 필요)
        val logSimplificationToggle = findViewById<android.widget.Switch>(R.id.switch_log_simplification)
        logSimplificationToggle?.isChecked = LogManager.isLogSimplified()
        logSimplificationToggle?.setOnCheckedChangeListener { _, isChecked ->
            LogManager.setLogSimplified(isChecked)
        }
    }
}
