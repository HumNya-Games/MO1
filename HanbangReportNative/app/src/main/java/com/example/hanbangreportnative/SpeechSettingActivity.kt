package com.example.hanbangreportnative

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout
import android.view.View
import android.widget.TextView
import android.widget.ImageView
import android.content.SharedPreferences
import android.content.Intent
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.EditText
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton

class SpeechSettingActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var mainLayout: LinearLayout
    private lateinit var voiceChangeSection: LinearLayout
    private lateinit var voiceImproveSection: LinearLayout
    private lateinit var autoAdjustSection: LinearLayout
    
    // UI 상태 관리
    private var currentWakeWord = "신고"
    private var isVoiceRecognitionActive = false
    private var isNoiseMeasurementActive = false
    
    // 오디오 레벨 관리
    private lateinit var audioLevelManager: AudioLevelManager
    private val mainHandler = Handler(Looper.getMainLooper())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_setting)
        findViewById<BottomNavBar>(R.id.bottom_nav_bar).setCurrentScreen(2)

        // LogManager 초기화
        LogManager.init(this)
        
        prefs = getSharedPreferences("speech_settings", MODE_PRIVATE)
        
        // 기본 UI 초기화
        initializeUI()
        
        // 저장된 설정 로드
        loadSavedSettings()
        
        // 이벤트 리스너 설정
        setupEventListeners()
        
        // 오디오 레벨 매니저 초기화
        audioLevelManager = AudioLevelManager(this)
    }
    
    private fun initializeUI() {
        mainLayout = findViewById(R.id.main_layout)
        
        // 1. 신고 음성 변경 하기 섹션
        voiceChangeSection = findViewById(R.id.voice_change_section)
        
        // 2. 신고 음성 인식률 높이기 섹션
        voiceImproveSection = findViewById(R.id.voice_improve_section)
        
        // 3. 음성 감지 세팅 자동 조절 섹션
        autoAdjustSection = findViewById(R.id.auto_adjust_section)
        
        // 현재 신고 음성 표시 업데이트
        updateCurrentWakeWordDisplay()
    }
    
    private fun loadSavedSettings() {
        // 저장된 웨이크워드 로드
        currentWakeWord = prefs.getString("current_wake_word", "신고") ?: "신고"
        
        // 마이크 민감도 설정 로드
        val micSensitivity = prefs.getInt("mic_sensitivity", 3500)
        val smallSoundThreshold = prefs.getInt("small_sound_threshold", 3500)
        val largeSoundThreshold = prefs.getInt("large_sound_threshold", 3500)
        
        // UI에 설정값 반영
        updateSensitivityDisplays(micSensitivity, smallSoundThreshold, largeSoundThreshold)
    }
    
    private fun setupEventListeners() {
        // 신고 음성 변경 버튼
        findViewById<Button>(R.id.btn_change_wake_word).setOnClickListener {
            showWakeWordChangePopup()
        }
        
        // 신고 음성 인식률 높이기 버튼
        findViewById<Button>(R.id.btn_improve_recognition).setOnClickListener {
            startVoiceRecognitionImprovement()
        }
        
        // 음성 감지 세팅 자동 조절 버튼
        findViewById<Button>(R.id.btn_auto_adjust).setOnClickListener {
            showNoiseMeasurementPopup1()
        }
        
        // 민감도 조절 SeekBar 설정
        setupSensitivityControls()
    }
    
    private fun updateCurrentWakeWordDisplay() {
        val wakeWordText = findViewById<TextView>(R.id.text_current_wake_word)
        wakeWordText.text = currentWakeWord
    }
    
    private fun updateSensitivityDisplays(micSensitivity: Int, smallSound: Int, largeSound: Int) {
        // 마이크 민감도 (1-10 스케일로 변환)
        val micDisplay = findViewById<TextView>(R.id.text_mic_sensitivity_display)
        val micValue = ((micSensitivity / 500.0) * 10).toInt().coerceIn(1, 10)
        micDisplay.text = micValue.toString()
        
        // 작은 소리 감지
        val smallSoundDisplay = findViewById<TextView>(R.id.text_small_sound_display)
        val smallValue = ((smallSound / 500.0) * 10).toInt().coerceIn(1, 10)
        smallSoundDisplay.text = smallValue.toString()
        
        // 큰 소리 감지
        val largeSoundDisplay = findViewById<TextView>(R.id.text_large_sound_display)
        val largeValue = ((largeSound / 500.0) * 10).toInt().coerceIn(1, 10)
        largeSoundDisplay.text = largeValue.toString()
    }
    
    private fun setupSensitivityControls() {
        // 마이크 민감도 SeekBar
        val micSeekBar = findViewById<SeekBar>(R.id.seekbar_mic_sensitivity)
        micSeekBar.max = 5000
        micSeekBar.progress = prefs.getInt("mic_sensitivity", 3500)
        micSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    prefs.edit().putInt("mic_sensitivity", progress).apply()
                    updateSensitivityDisplays(progress, 
                        prefs.getInt("small_sound_threshold", 3500),
                        prefs.getInt("large_sound_threshold", 3500))
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 작은 소리 감지 SeekBar
        val smallSoundSeekBar = findViewById<SeekBar>(R.id.seekbar_small_sound)
        smallSoundSeekBar.max = 5000
        smallSoundSeekBar.progress = prefs.getInt("small_sound_threshold", 3500)
        smallSoundSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    prefs.edit().putInt("small_sound_threshold", progress).apply()
                    updateSensitivityDisplays(
                        prefs.getInt("mic_sensitivity", 3500),
                        progress,
                        prefs.getInt("large_sound_threshold", 3500))
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 큰 소리 감지 SeekBar
        val largeSoundSeekBar = findViewById<SeekBar>(R.id.seekbar_large_sound)
        largeSoundSeekBar.max = 5000
        largeSoundSeekBar.progress = prefs.getInt("large_sound_threshold", 3500)
        largeSoundSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    prefs.edit().putInt("large_sound_threshold", progress).apply()
                    updateSensitivityDisplays(
                        prefs.getInt("mic_sensitivity", 3500),
                        prefs.getInt("small_sound_threshold", 3500),
                        progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    // ====== 웨이크워드 변경 관련 ======
    private fun showWakeWordChangePopup() {
        // 기존 UI 숨기기
        hideMainUI()
        
        // 웨이크워드 변경 팝업 표시
        val popupView = layoutInflater.inflate(R.layout.popup_wake_word_change, null)
        val popupContainer = findViewById<FrameLayout>(R.id.popup_container)
        
        // 팝업을 중앙에 배치하기 위한 LayoutParams 설정
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = android.view.Gravity.CENTER
        
        popupContainer.addView(popupView, layoutParams)
        popupContainer.visibility = View.VISIBLE
        
        // 팝업 내부 이벤트 설정
        setupWakeWordChangePopupEvents(popupView)
    }
    
    private fun setupWakeWordChangePopupEvents(popupView: View) {
        // 현재 웨이크워드 표시
        val currentText = popupView.findViewById<TextView>(R.id.text_current_wake_word)
        currentText.text = currentWakeWord
        
        // 입력 필드 설정 (3글자 제한)
        val inputField = popupView.findViewById<EditText>(R.id.edit_wake_word_input)
        inputField.setText(currentWakeWord)
        
        // 3글자 제한 설정
        inputField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.length ?: 0 > 3) {
                    inputField.setText(s?.substring(0, 3))
                    inputField.setSelection(3)
                }
            }
        })
        
        // 키보드에서 Go 버튼 처리
        inputField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                saveWakeWord(inputField.text.toString())
                return@setOnEditorActionListener true
            }
            false
        }
        
        // 저장 버튼
        popupView.findViewById<Button>(R.id.btn_save).setOnClickListener {
            saveWakeWord(inputField.text.toString())
        }
        
        // 취소 버튼
        popupView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            hidePopup()
            showMainUI()
        }
    }
    
    private fun saveWakeWord(newWakeWord: String) {
        if (newWakeWord.isBlank()) {
            return
        }
        
        // 웨이크워드 변경
        currentWakeWord = newWakeWord
        prefs.edit().putString("current_wake_word", newWakeWord).apply()
        
        // 기존 보정 데이터 삭제
        CorrectionDataStore.clearAllCorrectionData(this)
        
        // WakeWordStore 업데이트
        WakeWordStore.wakeWordList.clear()
        WakeWordStore.wakeWordList.add(newWakeWord)
        WakeWordStore.save(this)
        
        // UI 업데이트
        updateCurrentWakeWordDisplay()
        
        // 팝업 닫기
        hidePopup()
        showMainUI()
    }
    
    // ====== 음성 인식률 향상 관련 ======
    private fun startVoiceRecognitionImprovement() {
        // 기존 UI 숨기기
        hideMainUI()
        
        // 음성 인식 화면 표시
        val recognitionView = layoutInflater.inflate(R.layout.layout_voice_recognition, null)
        val popupContainer = findViewById<FrameLayout>(R.id.popup_container)
        
        // 팝업을 중앙에 배치하기 위한 LayoutParams 설정
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = android.view.Gravity.CENTER
        
        popupContainer.addView(recognitionView, layoutParams)
        popupContainer.visibility = View.VISIBLE
        
        // 음성 인식 시작
        startVoiceRecognitionProcess(recognitionView)
    }
    
    private fun startVoiceRecognitionProcess(view: View) {
        isVoiceRecognitionActive = true
        var remainingCount = 5
        
        // TTS 안내 시작
        val guideText = "화면에 보이는 신고 음성을 말씀해 주세요"
        speakTTS(guideText)
        
        // 안내 음성이 끝난 후 음성 인식 시작
        audioLevelManager.waitForVoiceCompletion(guideText) {
            if (isVoiceRecognitionActive) {
                startVoiceRecognition(view, remainingCount)
            }
        }
    }
    
    private fun startVoiceRecognition(view: View, remainingCount: Int) {
        if (!isVoiceRecognitionActive || remainingCount <= 0) {
            finishVoiceRecognition()
            return
        }
        
        // 마이크 아이콘 활성화
        val micIcon = view.findViewById<ImageView>(R.id.icon_mic)
        micIcon.setImageResource(R.drawable.mike_active)
        
        // 남은 횟수 업데이트
        val countText = view.findViewById<TextView>(R.id.text_remaining_count)
        countText.text = "남은 횟수 : $remainingCount"
        
        // 음성 인식 시작 (SpeechService 연동)
        startSpeechRecognition(view, remainingCount)
        
        // 그만하기 버튼
        view.findViewById<Button>(R.id.btn_stop_recognition).setOnClickListener {
            isVoiceRecognitionActive = false
            finishVoiceRecognition()
        }
    }
    
    private fun startSpeechRecognition(view: View, remainingCount: Int) {
        // SpeechService에서 음성 인식 시작
        val speechService = SpeechService.getInstance(this)
        speechService?.let { service ->
            // 음성 인식 결과 콜백 설정
            service.setRecognitionCallback { recognizedText ->
                if (recognizedText.isNotBlank()) {
                    // 보정 데이터에 추가
                    CorrectionDataStore.addCorrectionData(this, recognizedText)
                    
                    // 다음 인식으로 진행
                    mainHandler.post {
                        continueVoiceRecognition(view, remainingCount - 1)
                    }
                }
            }
            
            // 음성 인식 시작 (간단한 시뮬레이션)
            mainHandler.postDelayed({
                if (isVoiceRecognitionActive) {
                    // 실제로는 SpeechService의 음성 인식 결과를 받아야 함
                    // 현재는 시뮬레이션으로 처리
                    val simulatedText = currentWakeWord
                    CorrectionDataStore.addCorrectionData(this, simulatedText)
                    continueVoiceRecognition(view, remainingCount - 1)
                }
            }, 3000) // 3초 후 시뮬레이션 결과
        }
    }
    
    private fun continueVoiceRecognition(view: View, remainingCount: Int) {
        if (!isVoiceRecognitionActive || remainingCount <= 0) {
            finishVoiceRecognition()
            return
        }
        
        // 마이크 아이콘 비활성화
        val micIcon = view.findViewById<ImageView>(R.id.icon_mic)
        micIcon.setImageResource(R.drawable.mike)
        
        // 남은 횟수 업데이트
        val countText = view.findViewById<TextView>(R.id.text_remaining_count)
        countText.text = "남은 횟수 : $remainingCount"
        
        // 안내 음성 재생 후 다음 인식 시작
        val guideText = "한번 더 말씀해 주세요"
        speakTTS(guideText)
        
        audioLevelManager.waitForVoiceCompletion(guideText) {
            if (isVoiceRecognitionActive) {
                startVoiceRecognition(view, remainingCount)
            }
        }
    }
    
    private fun finishVoiceRecognition() {
        isVoiceRecognitionActive = false
        hidePopup()
        showMainUI()
    }
    
    // ====== 소음 측정 관련 ======
    private fun showNoiseMeasurementPopup1() {
        // 기존 UI 숨기기
        hideMainUI()
        
        // 소음 측정 안내 팝업1 표시
        val popupView = layoutInflater.inflate(R.layout.popup_noise_measurement_1, null)
        val popupContainer = findViewById<FrameLayout>(R.id.popup_container)
        
        // 팝업을 중앙에 배치하기 위한 LayoutParams 설정
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = android.view.Gravity.CENTER
        
        popupContainer.addView(popupView, layoutParams)
        popupContainer.visibility = View.VISIBLE
        
        // 시작 버튼 이벤트
        popupView.findViewById<Button>(R.id.btn_start_noise_measurement).setOnClickListener {
            startNoiseMeasurement()
        }
    }
    
    private fun startNoiseMeasurement() {
        isNoiseMeasurementActive = true
        
        // 소음 측정 안내 팝업2 표시
        val popupView = layoutInflater.inflate(R.layout.popup_noise_measurement_2, null)
        val popupContainer = findViewById<FrameLayout>(R.id.popup_container)
        popupContainer.removeAllViews()
        
        // 팝업을 중앙에 배치하기 위한 LayoutParams 설정
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = android.view.Gravity.CENTER
        
        popupContainer.addView(popupView, layoutParams)
        
        // TTS 안내
        val guideText = "주행 환경 소음을 측정합니다. 10초간 조용히 계세요."
        speakTTS(guideText)
        
        // 안내 음성이 끝난 후 소음 측정 시작
        audioLevelManager.waitForVoiceCompletion(guideText) {
            if (isNoiseMeasurementActive) {
                startNoiseMeasurementProcess(popupView)
            }
        }
    }
    
    private fun startNoiseMeasurementProcess(view: View) {
        var countdown = 10
        
        val countdownText = view.findViewById<TextView>(R.id.text_countdown)
        val timer = object : android.os.CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (isNoiseMeasurementActive) {
                    countdownText.text = countdown.toString()
                    countdown--
                } else {
                    cancel()
                }
            }
            
            override fun onFinish() {
                if (isNoiseMeasurementActive) {
                    finishNoiseMeasurement()
                }
            }
        }
        timer.start()
        
        // 실제 오디오 레벨 측정 시작
        audioLevelManager.measureBackgroundNoise(10) { backgroundRms ->
            mainHandler.post {
                // 백그라운드 소음 측정 완료 후 음성 피크 측정
                audioLevelManager.measureVoicePeak(5) { voicePeak ->
                    mainHandler.post {
                        // 자동 임계값 계산 및 적용
                        val (micSensitivity, smallSoundThreshold, largeSoundThreshold) = 
                            audioLevelManager.calculateAutoThresholds(backgroundRms, voicePeak)
                        
                        // 설정 저장
                        prefs.edit()
                            .putInt("mic_sensitivity", micSensitivity)
                            .putInt("small_sound_threshold", smallSoundThreshold)
                            .putInt("large_sound_threshold", largeSoundThreshold)
                            .apply()
                        
                        // UI 업데이트
                        updateSensitivityDisplays(micSensitivity, smallSoundThreshold, largeSoundThreshold)
                    }
                }
            }
        }
    }
    
    private fun finishNoiseMeasurement() {
        isNoiseMeasurementActive = false
        val guideText = "소음 측정이 완료되었습니다. 이제 신고 음성을 3회 말씀해주세요."
        speakTTS(guideText)
        
        // 안내 음성이 끝난 후 음성 인식 화면으로 전환
        audioLevelManager.waitForVoiceCompletion(guideText) {
            startVoiceRecognitionImprovement()
        }
    }
    
    // ====== UI 제어 메서드들 ======
    private fun hideMainUI() {
        mainLayout.visibility = View.GONE
    }
    
    private fun showMainUI() {
        mainLayout.visibility = View.VISIBLE
    }
    
    private fun hidePopup() {
        val popupContainer = findViewById<FrameLayout>(R.id.popup_container)
        popupContainer.removeAllViews()
        popupContainer.visibility = View.GONE
    }
    
    // ====== TTS 관련 ======
    private fun speakTTS(text: String) {
        // SpeechService의 TTS 기능 활용
        SpeechService.getInstance(this)?.speakTTS(text)
    }
    
    override fun onBackPressed() {
        if (isVoiceRecognitionActive || isNoiseMeasurementActive) {
            // 진행 중인 작업 중단
            isVoiceRecognitionActive = false
            isNoiseMeasurementActive = false
            hidePopup()
            showMainUI()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // 오디오 레벨 측정 중지
        audioLevelManager.stopAudioLevelMeasurement()
        
        // 진행 중인 작업 중단
        isVoiceRecognitionActive = false
        isNoiseMeasurementActive = false
    }
}
