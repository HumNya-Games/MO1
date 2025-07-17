package com.example.hanbangreportnative

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener as VoskRecognitionListener
import org.vosk.android.SpeechService as VoskSpeechService
import java.util.Locale
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException
import java.util.zip.ZipInputStream
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.media.AudioManager
import android.media.AudioRecord
import android.speech.tts.UtteranceProgressListener

// 오인식 트리거(유사어) 리스트 전역 관리 싱글톤
object SimilarTriggerStore {
    private const val PREFS_NAME = "speech_settings"
    private const val KEY_SIMILAR_LIST = "trigger_similar_list"
    private val defaultList = listOf(
        "신구", "진구", "신후시인후", "시인",
        "신고요", "신고오", "싱고", "신고우", "신고오요",
        "신곡", "신공"
    )
    var triggerSimilarList: MutableList<String> = defaultList.toMutableList()

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val csv = prefs.getString(KEY_SIMILAR_LIST, null)
        triggerSimilarList = if (csv.isNullOrBlank()) {
            defaultList.toMutableList()
        } else {
            csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        }
    }
    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SIMILAR_LIST, triggerSimilarList.joinToString(",")).apply()
    }
    fun add(word: String, context: Context? = null) {
        if (!triggerSimilarList.contains(word)) {
            triggerSimilarList.add(word)
            context?.let { save(it) }
        }
    }
    fun remove(word: String, context: Context? = null) {
        if (triggerSimilarList.remove(word)) {
            context?.let { save(it) }
        }
    }
    fun resetToDefault(context: Context? = null) {
        triggerSimilarList = defaultList.toMutableList()
        context?.let { save(it) }
    }
}

// 웨이크워드 리스트 전역 관리 싱글톤
object WakeWordStore {
    private const val PREFS_NAME = "speech_settings"
    private const val KEY_WAKEWORD_LIST = "wakeword_list"
    private val defaultList = listOf(
        "신고", "신구", "진구", "신후시인후", "시인",
        "신고요", "신고오", "싱고", "신고우", "신고오요",
        "신곡", "신공", "신고 읍", "신고해", "신고해줘", "신고해라",
        "신고요", "신고합니다", "신고해주세요", "신고해주세요요"
    )
    var wakeWordList: MutableList<String> = defaultList.toMutableList()

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val csv = prefs.getString(KEY_WAKEWORD_LIST, null)
        wakeWordList = if (csv.isNullOrBlank()) {
            defaultList.toMutableList()
        } else {
            csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        }
    }
    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_WAKEWORD_LIST, wakeWordList.joinToString(",")).apply()
    }
    fun add(word: String, context: Context? = null) {
        if (!wakeWordList.contains(word)) {
            wakeWordList.add(word)
            context?.let { save(it) }
        }
    }
    fun remove(word: String, context: Context? = null) {
        if (wakeWordList.remove(word)) {
            context?.let { save(it) }
        }
    }
    fun resetToDefault(context: Context? = null) {
        wakeWordList = defaultList.toMutableList()
        context?.let { save(it) }
    }
}

// 앱 종료 명령 리스트 전역 관리 싱글톤
object AppExitWordStore {
    private const val PREFS_NAME = "speech_settings"
    private const val KEY_APPEXIT_LIST = "appexit_list"
    private val defaultList = listOf(
        "앱종료", "앱종요", "액중요", "액종료", "앱종", "앱종료해", "앱종료해라",
        "앱종료해줘", "앱종료요", "앱종료합니다", "앱종료해주세요", "앱종료해주세요요"
    )
    var appExitWordList: MutableList<String> = defaultList.toMutableList()

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val csv = prefs.getString(KEY_APPEXIT_LIST, null)
        appExitWordList = if (csv.isNullOrBlank()) {
            defaultList.toMutableList()
        } else {
            csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        }
    }
    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_APPEXIT_LIST, appExitWordList.joinToString(",")).apply()
    }
    fun add(word: String, context: Context? = null) {
        if (!appExitWordList.contains(word)) {
            appExitWordList.add(word)
            context?.let { save(it) }
        }
    }
    fun remove(word: String, context: Context? = null) {
        if (appExitWordList.remove(word)) {
            context?.let { save(it) }
        }
    }
    fun resetToDefault(context: Context? = null) {
        appExitWordList = defaultList.toMutableList()
        context?.let { save(it) }
    }
}

class SpeechService(private val context: Context, private val callback: Callback) {
    interface Callback {
        fun onReportTrigger() //신고리거 인식 시 호출
        fun onReportContentResult(content: String) // 신고 내용(최대12 결과
        fun onAppExit() // "앱 종료" 인식 시 호출
        fun onError(error: String)
    }

    companion object {
        private const val TAG = "SpeechService"
        private const val MAX_CONSECUTIVE_EMPTY_RESULTS = 5
        private const val WAKEWORD_TIMEOUT_MS: Long = 30000
        private const val REPORT_CONTENT_TIMEOUT_MS: Long = 15000
        private const val MAX_TYPE_LENGTH = 10
        private const val REPORT_GUIDE_TEXT = "신고 내용을 말씀해 주세요."
        private const val REPORT_SAVE_TEXT = "신고 내용이 저장되었습니다."
    }

    // ====== 핵심 상태 관리 ======
    private enum class RecognitionState {
        IDLE,           // 대기 상태
        WAKEWORD,       // 웨이크워드 인식 중
        REPORT_CONTENT, // 신고 내용 인식 중
        TTS_SPEAKING    // TTS 안내 중
    }
    
    private var currentState = RecognitionState.IDLE
    private var isServiceActive = false // 서비스 활성화 상태
    private var isAppInForeground = false // 앱 포그라운드 상태

    // ====== Vosk 관련 변수 ======
    private var voskModel: Model? = null
    private var currentRecognizer: Recognizer? = null
    private var currentSpeechService: VoskSpeechService? = null
    private var isVoskReady = false

    // ====== TTS 관련 변수 ======
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    // ====== 타이머 관리 ======
    private val mainHandler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var silenceRunnable: Runnable? = null
    private var lastRecognizedText = ""
    private var consecutiveEmptyResults = 0
    private var restartDelay = 1000
    private var speechLanguage: String = "ko-KR"
    private var reportTriggerWord: String = "신고"
    private var appExitWord: String = "앱 종료"

    // ====== 설정값 로드 ======
    private var currentMicSensitivity = 2000
    init {
        loadSettings()
        initTTS()
    }

    private fun loadSettings() {
        val prefs = context.getSharedPreferences("speech_settings", Context.MODE_PRIVATE)
        currentMicSensitivity = prefs.getInt("mic_sensitivity", 2000)
        SimilarTriggerStore.load(context)
        WakeWordStore.load(context)
        AppExitWordStore.load(context)
    }

    // ====== TTS 초기화 ======
    private fun initTTS() {
        if (tts == null) {
            tts = TextToSpeech(context) { status ->
                ttsReady = (status == TextToSpeech.SUCCESS)
                if (ttsReady) {
                    tts?.language = Locale.forLanguageTag(speechLanguage)
                }
            }
        }
    }

    // ====== Vosk 모델 초기화 ======
    private fun initVoskModel() {
        if (voskModel == null) {
            try {
                val modelDir = File(context.filesDir, "model-ko")
                if (modelDir.exists()) deleteDir(modelDir)
                
                val assetList = context.assets.list("")?.toList() ?: emptyList()
                val copied = if (assetList.contains("model-ko.zip")) {
                    unzipModel(context, "model-ko.zip", modelDir)
                } else {
                    copyAssetFolder(context, "model-ko", modelDir.absolutePath)
                }
                
                if (!copied) throw IOException("Vosk 모델 복사/압축 해제 실패")
                if (!validateModelFiles(modelDir)) {
                    throw IOException("Vosk 모델 파일 검증실패")
                }
                
                voskModel = Model(modelDir.absolutePath)
                isVoskReady = true
                Log.d(TAG, "Vosk 모델 로딩 완료")
            } catch (e: Exception) {
                isVoskReady = false
                Log.e(TAG, "Vosk 모델 로딩 실패: ${e.message}")
                callback.onError(e.message ?: "Vosk 모델 로딩 실패")
            }
        }
    }

    // ====== 서비스 제어 ======
    fun start() {
        Log.d(TAG, "음성 인식 서비스 시작 요청")
        
        // 서비스 활성화 상태 설정
        isServiceActive = true
        currentState = RecognitionState.IDLE
        
        if (!canStartRecognition()) {
            Log.d(TAG, "음성 인식 시작 조건 불만족 - 조건 확인")
            Log.d(TAG, "isServiceActive: $isServiceActive, isAppInForeground: $isAppInForeground, currentState: $currentState")
            return
        }
        
        Log.d(TAG, "음성 인식 서비스 시작")
        startWakeWordRecognition()
    }

    fun stop() {
        Log.d(TAG, "음성 인식 서비스 중지")
        isServiceActive = false
        cleanupRecognition()
        resetState()
    }

    fun destroy() {
        stop()
        tts?.shutdown()
        tts = null
    }

    // ====== 상태 설정 ======
    fun setFloatingBallActive(active: Boolean) {
        Log.d(TAG, "플로팅 볼 활성화 상태: $active")
        if (!active) {
            stop()
        }
    }

    fun setAppForegroundState(inForeground: Boolean) {
        Log.d(TAG, "앱 포그라운드 상태: $inForeground")
        isAppInForeground = inForeground
        if (inForeground) {
            stop()
        }
    }

    // ====== 음성 인식 시작 조건 체크 ======
    private fun canStartRecognition(): Boolean {
        return isServiceActive && !isAppInForeground && currentState == RecognitionState.IDLE
    }

    // ====== 웨이크워드 인식 시작 ======
    private fun startWakeWordRecognition() {
        if (currentState != RecognitionState.IDLE) return
        
        Log.d(TAG, "워드 인식 시작")
        currentState = RecognitionState.WAKEWORD
        
        if (!isVoskReady) {
            initVoskModel()
            if (!isVoskReady) {
                resetState()
                return
            }
        }
        
        cleanupRecognition()
        createRecognizer()
        startRecognition(WAKEWORD_TIMEOUT_MS)
    }

    // ====== 웨이크워드 체크 ======
    private fun isWakeWord(text: String): Boolean {
        val normalized = text.replace("[^가-힣a-zA-Z0-9]".toRegex(), "").lowercase()
        
        // 정확한 매칭
        for (word in WakeWordStore.wakeWordList) {
            val normalizedWord = word.replace("[^가-힣a-zA-Z0-9]".toRegex(), "").lowercase()
            if (normalized.contains(normalizedWord)) return true
        }
        
        // 유사어 체크 (SimilarTriggerStore)
        val words = normalized.split(" ").filter { it.isNotBlank() }
        for (word in words) {
            if (word.length >= 2) {
                for (triggerWord in WakeWordStore.wakeWordList) {
                    val normalizedTrigger = triggerWord.replace("[^가-힣a-zA-Z0-9]".toRegex(), "").lowercase()
                    if (levenshtein(word, normalizedTrigger) <= 1) return true
                }
                if (SimilarTriggerStore.triggerSimilarList.contains(word)) return true
            }
        }
        
        return false
    }

    // ====== 앱 종료 명령 체크 ======
    private fun isAppExitCommand(text: String): Boolean {
        val normalized = text.replace("[^가-힣a-zA-Z0-9]".toRegex(), "").lowercase()
        
        // 정확한 매칭
        for (word in AppExitWordStore.appExitWordList) {
            val normalizedWord = word.replace("[^가-힣a-zA-Z0-9]".toRegex(), "").lowercase()
            if (normalized.contains(normalizedWord)) return true
        }
        
        // 유사어 체크 (Levenshtein 거리)
        val words = normalized.split(" ").filter { it.isNotBlank() }
        for (word in words) {
            if (word.length >= 2) {
                for (exitWord in AppExitWordStore.appExitWordList) {
                    val normalizedExit = exitWord.replace("[^가-힣a-zA-Z0-9]".toRegex(), "").lowercase()
                    if (levenshtein(word, normalizedExit) <= 1) return true
                }
            }
        }
        
        return false
    }

    // ====== Levenshtein 거리 계산 ======
    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
                else minOf(dp[i - 1][j - 1], dp[i][j - 1], dp[i - 1][j]) + 1
            }
        }
        return dp[a.length][b.length]
    }

    // ====== 신고 내용 인식 시작 ======
    private fun startReportContentRecognition() {
        if (!canStartRecognition()) {
            Log.d(TAG, "신고 내용 인식 시작 조건 불만족")
            return
        }
        
        try {
            currentState = RecognitionState.REPORT_CONTENT
            Log.d(TAG, "신고 내용 인식 시작")
            // 신고 내용용 Recognizer 생성
            createRecognizer()
            startRecognition(REPORT_CONTENT_TIMEOUT_MS)
            
        } catch (e: Exception) {
            Log.e(TAG, "신고 내용 인식 시작 오류: ${e.message}")
            currentState = RecognitionState.IDLE
        }
    }

    // ====== Recognizer 생성 ======
    private fun createRecognizer() {
        currentRecognizer = Recognizer(voskModel, 16000f)
        try { 
            currentRecognizer?.setWords(true) 
            currentRecognizer?.setPartialWords(true) 
        } catch (_: Exception) {}
        
        currentSpeechService = VoskSpeechService(currentRecognizer, 16000f)
    }

    // ====== 음성 인식 시작 ======
    private fun startRecognition(timeoutMs: Long) {
        currentSpeechService?.startListening(object : VoskRecognitionListener {
            override fun onResult(hypothesis: String?) {
                handleRecognitionResult(hypothesis, isFinal = true)
            }
            
            override fun onPartialResult(hypothesis: String?) {
                handleRecognitionResult(hypothesis, isFinal = false)
            }
            
            override fun onFinalResult(hypothesis: String?) {
                handleRecognitionResult(hypothesis, isFinal = true)
            }
            
            override fun onError(e: java.lang.Exception?) {
                Log.e(TAG, e.toString())
                handleRecognitionError()
            }
            
            override fun onTimeout() {
                Log.d(TAG, "음성 인식 타임아웃")
                handleRecognitionTimeout()
            }
        })
        
        // 타임아웃 설정
        timeoutRunnable = Runnable {
            Log.d(TAG, "타임아웃 발생")
            handleRecognitionTimeout()
        }
        mainHandler.postDelayed(timeoutRunnable!!, timeoutMs)
    }

    // ====== 인식 결과 처리 ======
    private fun handleRecognitionResult(hypothesis: String?, isFinal: Boolean) {
        val recognizedText = extractText(hypothesis)?.trim() ?: ""
        // 빈 결과는 무시하고 계속 인식 (무한 인식 유지)
        if (recognizedText.isBlank()) {
            return
        }
        
        lastRecognizedText = recognizedText
        Log.d(TAG, "인식결과:$recognizedText (상태: $currentState)")
        
        when (currentState) {
            RecognitionState.WAKEWORD -> handleWakeWordResult(recognizedText, isFinal)
            RecognitionState.REPORT_CONTENT -> handleReportContentResult(recognizedText, isFinal)
            else -> {}
        }
    }

    // ====== 웨이크워드 결과 처리 ======
    private fun handleWakeWordResult(result: String, isFinal: Boolean) {
        if (!isFinal) return
        
        // 앱 종료 명령 체크 (기존 방식 복원)
        if (isAppExitCommand(result)) {
            Log.d(TAG, "앱 종료 명령 인식")
            callback.onAppExit()
            return
        }
        
        // 웨이크워드 체크 (기존 방식 복원)
        if (isWakeWord(result)) {
            Log.d(TAG, "웨이크워드 인식 성공: '$result'")
            callback.onReportTrigger()
            
            // TTS 안내 후 신고 내용 인식 시작
            speakGuideAndStartReportContent()
            return
        }
        
        // 웨이크워드가 아닌 경우 무시하고 계속 인식
        Log.d(TAG, "웨이크워드가 아님: $result")
    }

    // ====== 신고 내용 결과 처리 ======
    private fun handleReportContentResult(text: String, isFinal: Boolean) {
        if (isFinal || text.length >= 3) {
            Log.d(TAG, "신고 내용 인식 완료: '$text'")
            cleanupRecognition()
            
            val finalText = if (text.isBlank()) "내용 없음" else text.take(MAX_TYPE_LENGTH)
            callback.onReportContentResult(finalText)
            
            speakSaveCompleteAndRestart()
        }
    }

    // ====== 음성 인식 오류 처리 ======
    private fun handleRecognitionError() {
        Log.d(TAG, "인식 오류발생")
        cleanupRecognition()
        restartRecognition()
    }

    // ====== 음성 인식 타임아웃 처리 ======
    private fun handleRecognitionTimeout() {
        Log.d(TAG, "음성 인식 타임아웃")
        cleanupRecognition()
        
        when (currentState) {
            RecognitionState.WAKEWORD -> restartRecognition()
            RecognitionState.REPORT_CONTENT -> {
                val finalText = if (lastRecognizedText.isBlank()) "내용 없음" else lastRecognizedText.take(MAX_TYPE_LENGTH)
                callback.onReportContentResult(finalText)
                speakSaveCompleteAndRestart()
            }
            else -> restartRecognition()
        }
    }

    // ====== 재시작 로직 ======
    private fun restartRecognition() {
        if (currentState == RecognitionState.TTS_SPEAKING) {
            Log.d(TAG, "TTS 중이므로 재시작 생략")
            return
        }
        
        try {
            Log.d(TAG, "음성 인식 재시작")
            
            // 현재 인식 정리 후 즉시 재시작 (딜레이 없음)
            cleanupRecognition()
            
            if (canStartRecognition()) {
                startWakeWordRecognition()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "재시작 오류: ${e.message}")
        }
    }

    // ====== TTS 안내 및 신고 내용 인식 시작 ======
    private fun speakGuideAndStartReportContent() {
        currentState = RecognitionState.TTS_SPEAKING
        speak(REPORT_GUIDE_TEXT) {
            currentState = RecognitionState.IDLE
            startReportContentRecognition()
        }
    }

    // ====== TTS 저장 완료 안내 및 재시작 ======
    private fun speakSaveCompleteAndRestart() {
        currentState = RecognitionState.TTS_SPEAKING
        speak(REPORT_SAVE_TEXT) {
            currentState = RecognitionState.IDLE
            restartRecognition()
        }
    }

    // ====== TTS 제어 ======
    private fun speak(text: String, onComplete: (() -> Unit)? = null) {
        try {
            // TTS 시작 시 현재 STT 완전 정리
            cleanupRecognition()
            
            currentState = RecognitionState.TTS_SPEAKING
            Log.d(TAG, "TTS 시작: $text")
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utterance_${System.currentTimeMillis()}")
            
            // TTS 완료 콜백 설정
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "TTS 시작됨: $utteranceId")
                }
                
                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "TTS 완료됨: $utteranceId")
                    currentState = RecognitionState.IDLE
                    onComplete?.invoke()
                }
                
                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "TTS 오류: $utteranceId")
                    currentState = RecognitionState.IDLE
                    onComplete?.invoke()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "TTS 오류: ${e.message}")
            currentState = RecognitionState.IDLE
            onComplete?.invoke()
        }
    }

    // ====== 상태 초기화 ======
    private fun resetState() {
        currentState = RecognitionState.IDLE
        lastRecognizedText = ""
        consecutiveEmptyResults = 0
    }

    // ====== 음성 인식 정리 ======
    private fun cleanupRecognition() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        silenceRunnable?.let { mainHandler.removeCallbacks(it) }
        
        try {
            currentSpeechService?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "SpeechService stop error", e)
        }
        
        try {
            currentRecognizer?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Recognizer close error", e)
        }
        
        currentSpeechService = null
        currentRecognizer = null
    }

    // ====== 텍스트 추출 ======
    private fun extractText(hypothesis: String?): String? {
        if (hypothesis.isNullOrBlank()) return null
        
        return try {
            // JSON 파싱 시도
            val json = org.json.JSONObject(hypothesis)
            if (json.has("text")) {
                return json.getString("text")
            }
            if (json.has("partial")) {
                return json.getString("partial")
            }
            
            // 정규식으로 추출 시도
            val textRegex = Regex("text\\s*:\\s*\"(.*?)\"")
            val textMatch = textRegex.find(hypothesis)
            if (textMatch != null) {
                return textMatch.groups[1]?.value
            }
            
            val partialRegex = Regex("partial\\s*:\\s*\"(.*?)\"")
            val partialMatch = partialRegex.find(hypothesis)
            if (partialMatch != null) {
                return partialMatch.groups[1]?.value
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }

    // ====== 파일 시스템 유틸리티 ======
    private fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { deleteDir(it) }
        }
        return dir.delete()
    }

    private fun copyAssetFolder(context: Context, assetPath: String, destPath: String): Boolean {
        return try {
            val assets = context.assets
            val files = assets.list(assetPath) ?: return false
            File(destPath).mkdirs()
            for (file in files) {
                val assetFilePath = if (assetPath.isEmpty()) file else "$assetPath/$file"
                val destFilePath = "$destPath/$file"
                val fileList = assets.list(assetFilePath)
                if (fileList.isNullOrEmpty()) {
                    copyAssetFile(context, assetFilePath, destFilePath)
                } else {
                    copyAssetFolder(context, assetFilePath, destFilePath)
                }
            }
            true
        } catch (e: IOException) {
            Log.e(TAG, "[복사실패] $assetPath: ${e.message}", e)
            false
        }
    }

    private fun copyAssetFile(context: Context, assetPath: String, destPath: String) {
        context.assets.open(assetPath).use { input ->
            FileOutputStream(File(destPath)).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun unzipModel(context: Context, zipAssetName: String, destDir: File): Boolean {
        return try {
            destDir.mkdirs()
            context.assets.open(zipAssetName).use { input ->
                ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val file = File(destDir, entry.name)
                        if (entry.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.parentFile?.mkdirs()
                            FileOutputStream(file).use { output ->
                                zis.copyTo(output)
                            }
                        }
                        entry = zis.nextEntry
                    }
                }
            }
            Log.d(TAG, "[압축해제] $zipAssetName → ${destDir.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "[압축해제실패] $zipAssetName: ${e.message}", e)
            false
        }
    }

    private fun validateModelFiles(dir: File): Boolean {
        if (!dir.exists() || !dir.isDirectory) {
            Log.e(TAG, "[검증] model-ko 폴더가 존재하지 않거나 폴더가 아님: ${dir.absolutePath}")
            return false
        }
        val files = dir.listFiles()
        if (files == null) {
            Log.e(TAG, "[검증] model-ko 폴더를 읽을 수 없음: ${dir.absolutePath}")
            return false
        }
        val required = listOf("am", "conf", "graph", "ivector")
        val folderNames = files.filter { it.isDirectory }.map { it.name }
        val found = required.count { folderNames.contains(it) }
        Log.d(TAG, "[검증] model-ko 내 폴더 목록: $folderNames, 필수 폴더 개수: $found")
        if (found < 3) {
            Log.e(TAG, "Vosk 모델 폴더에 필수 폴더가 부족합니다. (am, conf, graph, ivector 중 3개 이상 필요)")
            return false
        }
        return true
    }
} 