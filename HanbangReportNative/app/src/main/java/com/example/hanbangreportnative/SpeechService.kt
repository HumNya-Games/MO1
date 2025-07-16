package com.example.hanbangreportnative

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*
import kotlin.concurrent.timer
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager

class SpeechService(private val context: Context, private val callback: Callback) {
    interface Callback {
        fun onReportTrigger() // "신고" 트리거 인식 시 호출
        fun onReportContentResult(content: String) // 신고 내용(최대 12자) 결과
        fun onAppExit() // "앱 종료" 인식 시 호출
        fun onError(error: String)
    }

    companion object {
        // ====== 설정 포인트 ======
        const val SILENCE_TIMEOUT_MS = 3000 // 무음 종료 시간(3초, ms)
        const val MAX_TYPE_LENGTH = 12 // 신고 내용 최대 한글 12자
        var speechLanguage: String = "ko-KR" // 음성 인식/안내 언어
        var reportTriggerWord: String = "신고" // 트리거 키워드
        var appExitWord: String = "앱 종료" // 앱 종료 키워드
        // =========================
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isListening = false
    private var silenceTimer: Timer? = null
    private var isReportMode = false
    private var reportContent = ""
    private var reportContentTimer: Timer? = null
    private var reportContentTimeoutMs = 10000L // 안내 후 10초 대기
    private var lastErrorToastTime = 0L
    private var retryCount = 0
    private val MAX_RETRY = 5
    private var noMatchCount = 0
    private val MAX_NO_MATCH_COUNT = 3
    private var isTriggerHandled = false // 트리거 인식 후 중복 진입 방지
    private var isFirstSpeechRecognized = false // 최초 음성 인식 여부
    private var recognizedText = "" // 인식된 텍스트 누적
    private var originalVolume: Int? = null // TTS 볼륨 복원용
    private var isReportContentFinished = false // 최종 저장/알림/안내 1회만 보장

    fun start() {
        Log.d("SpeechService", "=== SpeechService.start() 호출됨 ===")
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("SpeechService", "SpeechRecognizer 사용 불가")
            callback.onError("이 기기에서는 음성 인식 기능을 사용할 수 없습니다. (서비스 미설치/미지원)")
            return
        }
        Log.d("SpeechService", "SpeechService 시작 - wake word 인식 모드")
        initTTS()
        safeInitSTT()
        Log.d("SpeechService", "startWakeWordListening() 호출 직전")
        startWakeWordListening() // wake word 인식 모드로 시작
        Log.d("SpeechService", "startWakeWordListening() 호출 완료")
    }

    fun stop() {
        Log.d("SpeechService", "stop() 호출")
        isListening = false
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e("SpeechService", "speechRecognizer destroy 오류: ${e.message}")
        }
    }

    fun destroy() {
        Log.d("SpeechService", "destroy() 호출")
        stop()
        // 서비스 완전 종료
        context?.let { ctx ->
            val intent = Intent(ctx, SpeechService::class.java)
            ctx.stopService(intent)
        }
    }

    private fun initTTS() {
        if (tts == null) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.forLanguageTag(speechLanguage)
                }
            }
        }
    }

    private fun safeInitSTT() {
        runOnMainThread {
            if (speechRecognizer != null) {
                try {
                    speechRecognizer?.destroy()
                    Log.d("SpeechService", "기존 SpeechRecognizer destroy 완료")
                } catch (e: Exception) {
                    Log.e("SpeechService", "SpeechRecognizer destroy 중 예외", e)
                }
                speechRecognizer = null
            }
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            Log.d("SpeechService", "SpeechRecognizer 새로 생성")
        }
    }

    private fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            Handler(Looper.getMainLooper()).post { action() }
        }
    }

    private fun normalize(text: String?): String {
        return text?.replace(" ", "")?.lowercase(Locale.getDefault()) ?: ""
    }

    // wake word("신고") 인식 대기: "신고"가 인식될 때까지 대기
    private fun startWakeWordListening() {
        runOnMainThread {
            Log.d("SpeechService", "=== startWakeWordListening() 진입 ===")
            if (isListening) {
                Log.w("SpeechService", "startWakeWordListening: 이미 isListening=true, 중복 호출 방지")
                return@runOnMainThread
            }
            // 기존 SpeechRecognizer 정리
            safeDestroySTT()
            isReportMode = false
            isTriggerHandled = false
            Log.d("SpeechService", "wake word 인식 설정 시작")
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 30000) // 30초로 증가 (시스템 최대값)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000) // 1초로 증가 (신고 내용과 동일)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 30000) // 30초로 증가 (시스템 최대값)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            Log.d("SpeechService", "Intent 설정 완료: $intent")
            val hasMicPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            Log.d("SpeechService", "startWakeWordListening: 마이크 권한=${hasMicPermission}")
            if (!hasMicPermission) {
                Log.e("SpeechService", "startWakeWordListening: 마이크 권한 없음, 음성 인식 불가")
                callback.onError("마이크 권한이 없습니다. 설정에서 권한을 허용해 주세요.")
                return@runOnMainThread
            }
            Log.d("SpeechService", "safeInitSTT() 호출 직전")
            safeInitSTT()
            Log.d("SpeechService", "safeInitSTT() 호출 완료, speechRecognizer=${speechRecognizer}")
            Log.d("SpeechService", "wake word 인식 리스너 설정")
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    Log.d("SpeechService", "[wake word] onResults: Bundle=$results")
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    Log.d("SpeechService", "[wake word] onResults: matches=$matches")
                    
                    if (!isTriggerHandled) {
                        val normalizedMatches = matches?.map { normalize(it) } ?: emptyList()
                        
                        // "앱 종료" 키워드 확인
                        if (normalizedMatches.any { it.contains(normalize(appExitWord)) }) {
                            isTriggerHandled = true
                            isListening = false
                            retryCount = 0
                            Log.d("SpeechService", "[wake word] 앱 종료 키워드 인식됨 → 앱 종료 (onResults)")
                            safeDestroySTT()
                            callback.onAppExit()
                            return
                        }
                        
                        // "신고" 키워드 확인
                        if (normalizedMatches.any { it.contains(normalize(reportTriggerWord)) }) {
                            isTriggerHandled = true
                            isListening = false
                            retryCount = 0
                            Log.d("SpeechService", "[wake word] 신고 wake word 인식됨 → 신고 내용 입력 단계로 (onResults)")
                            safeDestroySTT()
                            callback.onReportTrigger()
                            startReportContentFlow()
                            return
                        }
                    }
                    
                    Log.d("SpeechService", "[wake word] wake word 미인식, 다시 대기")
                    // wake word 미인식 시 다시 대기 (무한 반복 방지)
                    Handler(Looper.getMainLooper()).postDelayed({
                        startWakeWordListening()
                    }, 1000)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    Log.d("SpeechService", "[wake word] onPartialResults: Bundle=$partialResults")
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    Log.d("SpeechService", "[wake word] onPartialResults: partial=$partial")
                    
                    if (!isTriggerHandled) {
                        val normalizedPartial = partial?.map { normalize(it) } ?: emptyList()
                        
                        // "앱 종료" 키워드 확인
                        if (normalizedPartial.any { it.contains(normalize(appExitWord)) }) {
                            isTriggerHandled = true
                            isListening = false
                            retryCount = 0
                            Log.d("SpeechService", "[wake word] partial에서 앱 종료 키워드 인식됨 → 앱 종료 (onPartialResults)")
                            safeDestroySTT()
                            callback.onAppExit()
                            return
                        }
                        
                        // "신고" 키워드 확인
                        if (normalizedPartial.any { it.contains(normalize(reportTriggerWord)) }) {
                            isTriggerHandled = true
                            isListening = false
                            retryCount = 0
                            Log.d("SpeechService", "[wake word] partial에서 신고 wake word 인식됨 → 신고 내용 입력 단계로 (onPartialResults)")
                            safeDestroySTT()
                            callback.onReportTrigger()
                            startReportContentFlow()
                        }
                    }
                }
                override fun onEndOfSpeech() {
                    isListening = false
                    Log.d("SpeechService", "onEndOfSpeech: wake word 대기 종료")
                }
                override fun onError(error: Int) {
                    isListening = false
                    Log.e("SpeechService", "[wake word] onError: $error")
                    
                    if (!isTriggerHandled) {
                        when (error) {
                            7 -> { // SPEECH_TIMEOUT - 정상적인 타임아웃
                                Log.d("SpeechService", "[wake word] SPEECH_TIMEOUT(7) - 정상적인 타임아웃, 1초 후 재시작")
                                // 에러 7은 정상적인 타임아웃이므로 1초 후 재시작
                                Handler(Looper.getMainLooper()).postDelayed({
                                    startWakeWordListening()
                                }, 1000)
                            }
                            SpeechRecognizer.ERROR_NO_MATCH -> {
                                Log.d("SpeechService", "[wake word] NO_MATCH - 음성 미인식, 재시작")
                                startWakeWordListening()
                            }
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                                Log.e("SpeechService", "[wake word] 권한 부족")
                                callback.onError("마이크 권한이 없습니다. 설정에서 권한을 허용해 주세요.")
                            }
                            else -> {
                                Log.e("SpeechService", "[wake word] 기타 오류: $error")
                                // 기타 오류는 3초 후 재시작
                                Handler(Looper.getMainLooper()).postDelayed({
                                    startWakeWordListening()
                                }, 3000)
                            }
                        }
                    }
                }
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    Log.d("SpeechService", "onReadyForSpeech: wake word 대기 시작")
                }
                override fun onBeginningOfSpeech() { Log.d("SpeechService", "onBeginningOfSpeech") }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            isListening = true
            Log.d("SpeechService", "wake word 인식 시작 - startListening 호출")
            speechRecognizer?.startListening(intent)
            Log.d("SpeechService", "wake word 인식 시작 완료")
        }
    }

    // 트리거(신고) 인식 대기: "신고"가 인식될 때까지 무한 반복
    private fun startTriggerListening() {
        runOnMainThread {
            if (isListening) {
                Log.w("SpeechService", "startTriggerListening: 이미 isListening=true, 중복 호출 방지")
                return@runOnMainThread
            }
            isReportMode = false
            isTriggerHandled = false // 트리거 대기 진입 시 초기화
            // 트리거 대기 진입 시 재시작 카운터 초기화 (최대 재시작 횟수 도달 후 재진입하는 경우)
            if (retryCount >= MAX_RETRY) {
                retryCount = 0
                Log.d("SpeechService", "[트리거] 최대 재시작 횟수 도달 후 재진입, 카운터 리셋")
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 8000) // 8초로 증가
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500) // 0.5초로 감소
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 6000) // 6초 추가
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                // 오프라인 모드에서 네트워크 관련 설정 제거
                // putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // 이미 설정됨
            }
            val hasMicPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            Log.d("SpeechService", "startTriggerListening: 마이크 권한=${hasMicPermission}")
            if (!hasMicPermission) {
                Log.e("SpeechService", "startTriggerListening: 마이크 권한 없음, 음성 인식 불가")
                callback.onError("마이크 권한이 없습니다. 설정에서 권한을 허용해 주세요.")
                return@runOnMainThread
            }
            safeInitSTT()
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    Log.d("SpeechService", "[트리거] onResults: Bundle=$results")
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    Log.d("SpeechService", "[트리거] onResults: matches=$matches")
                    if (!isTriggerHandled && matches?.any { normalize(it).contains(normalize(reportTriggerWord)) } == true) {
                        isTriggerHandled = true
                        isListening = false
                        retryCount = 0 // 트리거 성공 시 카운터 리셋
                        // voiceRecognitionTimer?.cancel() // 트리거 인식 시 타이머 취소 // 이 타이머는 제거되었으므로 이 줄도 제거
                        Log.d("SpeechService", "[트리거] 신고 트리거 인식됨 → 신고 데이터 저장 및 내용 입력 단계로 (onResults)")
                        safeDestroySTT()
                        callback.onReportTrigger()
                        startReportContentFlow()
                    } else {
                        Log.d("SpeechService", "[트리거] 트리거 미인식, 무한 반복 대기")
                        startTriggerListening()
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    Log.d("SpeechService", "[트리거] onPartialResults: Bundle=$partialResults")
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    Log.d("SpeechService", "[트리거] onPartialResults: partial=$partial")
                    if (!isTriggerHandled && partial?.any { normalize(it).contains(normalize(reportTriggerWord)) } == true) {
                        isTriggerHandled = true
                        isListening = false
                        retryCount = 0 // 트리거 성공 시 카운터 리셋
                        // voiceRecognitionTimer?.cancel() // 트리거 인식 시 타이머 취소 // 이 타이머는 제거되었으므로 이 줄도 제거
                        Log.d("SpeechService", "[트리거] partial에서 신고 트리거 인식됨 → 신고 데이터 저장 및 내용 입력 단계로")
                        safeDestroySTT()
                        callback.onReportTrigger()
                        startReportContentFlow()
                    }
                }
                override fun onEndOfSpeech() {
                    isListening = false
                    Log.d("SpeechService", "onEndOfSpeech: 트리거 대기 종료")
                }
                override fun onError(error: Int) {
                    isListening = false
                    Log.e("SpeechService", "[트리거] onError: $error")
                    
                    if (!isTriggerHandled) {
                        when (error) {
                            7 -> { // SPEECH_TIMEOUT - 정상적인 타임아웃
                                Log.d("SpeechService", "[트리거] SPEECH_TIMEOUT(7) - 정상적인 타임아웃, 재시작")
                                // 1초 후 재시작 (너무 빈번한 재시작 방지)
                                Handler(Looper.getMainLooper()).postDelayed({
                                    startTriggerListening()
                                }, 1000)
                            }
                            SpeechRecognizer.ERROR_NO_MATCH -> {
                                Log.d("SpeechService", "[트리거] NO_MATCH - 음성 미인식, 재시작")
                                startTriggerListening()
                            }
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                                Log.e("SpeechService", "[트리거] 권한 부족")
                                callback.onError("마이크 권한이 없습니다. 설정에서 권한을 허용해 주세요.")
                            }
                            else -> {
                                Log.e("SpeechService", "[트리거] 기타 오류: $error")
                                // 기타 오류는 3초 후 재시작
                                Handler(Looper.getMainLooper()).postDelayed({
                                    startTriggerListening()
                                }, 3000)
                            }
                        }
                    }
                }
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    Log.d("SpeechService", "onReadyForSpeech: 트리거 대기 시작")
                }
                override fun onBeginningOfSpeech() { Log.d("SpeechService", "onBeginningOfSpeech") }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            isListening = true
            speechRecognizer?.startListening(intent)
        }
    }

    private fun restartTriggerListening() {
        runOnMainThread {
            if (!isReportMode && !isListening) {
                stopListening()
                startTriggerListening()
            } else {
                Log.w("SpeechService", "restartTriggerListening: isReportMode=$isReportMode, isListening=$isListening, 재시작 생략")
            }
        }
    }

    private fun startReportContentFlow() {
        runOnMainThread {
            isReportMode = true
            speak("신고 내용을 간단히 말씀해주세요") {
                startReportContentListening()
            }
        }
    }

    private fun speak(text: String, onDone: () -> Unit) {
        runOnMainThread {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val result = audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val isMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
            Log.d("SpeechService", "TTS speak: $text, 오디오포커스=$result, 볼륨=$volume, 음소거=$isMuted")
            // 볼륨 0일 때 임시로 5로 조정
            if (volume == 0) {
                originalVolume = 0
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 5, 0)
                Log.d("SpeechService", "TTS 볼륨 0 → 5로 임시 조정")
            } else {
                originalVolume = null
            }
            // 음소거 상태 안내
            if (isMuted) {
                callback.onError("음성 안내가 음소거 상태입니다. 볼륨을 확인해 주세요.")
                Log.w("SpeechService", "TTS 음소거 상태 안내 토스트")
            }
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d("SpeechService", "TTS onStart: $text")
                }
                override fun onDone(utteranceId: String?) {
                    Log.d("SpeechService", "TTS onDone: $text")
                    // 볼륨 복원
                    if (originalVolume != null) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume!!, 0)
                        Log.d("SpeechService", "TTS 볼륨 복원: $originalVolume")
                    }
                    runOnMainThread { onDone() }
                }
                override fun onError(utteranceId: String?) {
                    Log.d("SpeechService", "TTS onError: $text")
                    if (originalVolume != null) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume!!, 0)
                        Log.d("SpeechService", "TTS 볼륨 복원(오류): $originalVolume")
                    }
                    runOnMainThread { onDone() }
                }
            })
            Log.d("SpeechService", "TTS speak: $text (QUEUE_ADD 실험)")
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "tts_id")
        }
    }

    private fun startReportContentListening() {
        runOnMainThread {
            Log.d("SpeechService", "startReportContentListening() 진입");
            isReportContentFinished = false // 플래그 초기화
            if (isListening) {
                Log.w("SpeechService", "startReportContentListening: 이미 isListening=true, 중복 호출 방지")
                return@runOnMainThread
            }
            isListening = true
            safeDestroySTT()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            Log.d("SpeechService", "SpeechRecognizer 새로 생성(신고 내용)")
            reportContent = ""
            isFirstSpeechRecognized = false
            recognizedText = ""
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 10000)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("SpeechService", "onReadyForSpeech(신고 내용)")
                    startReportContentTimeout() // 10초 타이머 시작
                }
                override fun onBeginningOfSpeech() {
                    Log.d("SpeechService", "onBeginningOfSpeech(신고 내용)")
                    if (!isFirstSpeechRecognized) {
                        isFirstSpeechRecognized = true
                        cancelReportContentTimeout() // 10초 타이머 중지
                        startSilenceTimer() // 3초 무음 타이머 시작
                    } else {
                        resetSilenceTimer() // 추가 인식 시 3초 타이머 리셋
                    }
                }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    Log.d("SpeechService", "onEndOfSpeech(신고 내용)")
                    isListening = false
                    finishReportContentBySilence()
                }
                override fun onError(error: Int) {
                    Log.e("SpeechService", "onError(신고 내용): $error")
                    isListening = false
                    val hasMicPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            if (!hasMicPermission) {
                                showErrorToastOncePerInterval("마이크 권한이 없습니다. 설정에서 권한을 허용해 주세요.")
                                Log.e("SpeechService", "onError(신고 내용): 마이크 권한 없음")
                            } else {
                                showErrorToastOncePerInterval("음성 인식 엔진 오류입니다. 잠시 후 다시 시도해 주세요. (오류코드: $error)")
                                Log.e("SpeechService", "onError(신고 내용): 엔진 오류(권한은 있음) $error")
                                if (retryCount < MAX_RETRY) {
                                    retryCount++
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        restartTriggerListening()
                                    }, 3000)
                                }
                            }
                        }
                        SpeechRecognizer.ERROR_CLIENT, SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_SERVER -> {
                            showErrorToastOncePerInterval("음성 인식 엔진 또는 네트워크 오류입니다. 잠시 후 다시 시도해 주세요. (오류코드: $error)")
                            Log.e("SpeechService", "onError(신고 내용): 엔진/네트워크 오류 $error")
                            if (retryCount < MAX_RETRY) {
                                retryCount++
                                Handler(Looper.getMainLooper()).postDelayed({
                                    restartTriggerListening()
                                }, 3000)
                            }
                        }
                        7 -> {
                            Log.d("SpeechService", "onError(신고 내용): SPEECH_TIMEOUT(7) - 음성 인식 재시작")
                            // 에러 7은 단순 타임아웃이므로 음성 인식을 재시작
                            if (isReportMode) {
                                // 현재 음성 인식 중이면 재시작
                                safeDestroySTT()
                                startReportContentListening()
                            }
                        }
                        else -> {
                            showErrorToastOncePerInterval("음성 인식 오류: $error")
                            Log.e("SpeechService", "onError(신고 내용): 기타 오류 $error")
                            if (retryCount < MAX_RETRY) {
                                retryCount++
                                Handler(Looper.getMainLooper()).postDelayed({
                                    restartTriggerListening()
                                }, 3000)
                            }
                        }
                    }
                }
                override fun onResults(results: Bundle?) {
                    Log.d("SpeechService", "onResults(신고 내용): $results")
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        // 안내 문구 필터링
                        if (!text.contains("신고 내용을 간단히 말씀해 주세요")) {
                            recognizedText = text.take(MAX_TYPE_LENGTH)
                        }
                    }
                    finishReportContentBySilence()
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    Log.d("SpeechService", "onPartialResults(신고 내용): $partial")
                    if (!partial.isNullOrEmpty()) {
                        val text = partial[0].take(MAX_TYPE_LENGTH)
                        // 안내 문구 필터링
                        if (!text.contains("신고 내용을 간단히 말씀해 주세요")) {
                            recognizedText = text
                        }
                    }
                    if (isFirstSpeechRecognized) {
                        resetSilenceTimer()
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {
                    Log.d("SpeechService", "onEvent(신고 내용): eventType=$eventType, params=$params")
                }
            })
            speechRecognizer?.startListening(intent)
            Log.d("SpeechService", "speechRecognizer?.startListening(intent) 호출(신고 내용)")
        }
    }

    private fun startReportContentTimeout() {
        reportContentTimer?.cancel()
        reportContentTimer = timer(initialDelay = reportContentTimeoutMs, period = reportContentTimeoutMs) {
            if (isListening && !isFirstSpeechRecognized) {
                isListening = false
                finishReportContentByTimeout()
            }
            reportContentTimer?.cancel()
        }
    }
    private fun cancelReportContentTimeout() {
        reportContentTimer?.cancel()
    }
    private fun startSilenceTimer() {
        silenceTimer?.cancel()
        silenceTimer = timer(initialDelay = SILENCE_TIMEOUT_MS.toLong(), period = SILENCE_TIMEOUT_MS.toLong()) {
            if (isListening && isFirstSpeechRecognized) {
                isListening = false
                finishReportContentBySilence()
            }
            silenceTimer?.cancel()
        }
    }
    private fun resetSilenceTimer() {
        silenceTimer?.cancel()
        startSilenceTimer()
    }
    private fun cancelSilenceTimer() {
        silenceTimer?.cancel()
    }
    // 10초 타이머 만료: 아무 음성도 인식되지 않음
    private fun finishReportContentByTimeout() {
        runOnMainThread {
            if (isReportContentFinished) return@runOnMainThread
            isReportContentFinished = true
            cancelSilenceTimer()
            cancelReportContentTimeout()
            recognizedText = "내용 없음"
            Log.d("SpeechService", "[신고내용] 10초 타이머 만료, 내용없음 저장 및 wake word 대기 복귀")
            callback.onReportContentResult(recognizedText)
            speak("신고 내용이 저장되었습니다.") {
                isReportMode = false
                isListening = false
                // 1초 후 wake word 대기 모드로 복귀 (안정성 향상)
                Handler(Looper.getMainLooper()).postDelayed({
                    startWakeWordListening()
                }, 1000)
            }
        }
    }
    // 3초 무음: 인식된 내용 저장
    private fun finishReportContentBySilence() {
        runOnMainThread {
            if (isReportContentFinished) return@runOnMainThread
            isReportContentFinished = true
            cancelSilenceTimer()
            cancelReportContentTimeout()
            val result = if (recognizedText.isBlank()) "내용 없음" else recognizedText.take(MAX_TYPE_LENGTH)
            Log.d("SpeechService", "[신고내용] 3초 무음, 결과 저장: $result, wake word 대기 복귀")
            callback.onReportContentResult(result)
            speak("신고 내용이 저장되었습니다.") {
                isReportMode = false
                isListening = false
                // 1초 후 wake word 대기 모드로 복귀 (안정성 향상)
                Handler(Looper.getMainLooper()).postDelayed({
                    startWakeWordListening()
                }, 1000)
            }
        }
    }

    private fun stopListening() {
        runOnMainThread {
            if (!isListening) {
                Log.d("SpeechService", "stopListening: 이미 isListening=false, 중복 호출 방지")
                return@runOnMainThread
            }
            try {
                speechRecognizer?.stopListening()
                Log.d("SpeechService", "stopListening() 호출")
            } catch (e: Exception) {
                Log.e("SpeechService", "stopListening 중 예외", e)
            }
            isListening = false
        }
    }

    private fun showErrorToastOncePerInterval(error: String, intervalMs: Long = 3000) {
        val now = System.currentTimeMillis()
        if (now - lastErrorToastTime > intervalMs) {
            callback.onError(error)
            lastErrorToastTime = now
        }
    }

    private fun isFatalError(error: Int): Boolean {
        return error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
               error == SpeechRecognizer.ERROR_CLIENT ||
               error == SpeechRecognizer.ERROR_NETWORK ||
               error == SpeechRecognizer.ERROR_SERVER
    }

    private fun handleError(error: Int) {
        if (error == SpeechRecognizer.ERROR_NO_MATCH) {
            noMatchCount++
            Log.e("SpeechService", "NO_MATCH(7) 연속 발생: $noMatchCount")
            if (noMatchCount >= MAX_NO_MATCH_COUNT) {
                speak("음성이 인식되지 않아 음성 인식을 중단합니다.") {
                    stopListening()
                }
                noMatchCount = 0
                return
            }
        } else {
            noMatchCount = 0
        }
        // 기존 오류 처리
        startTriggerListening()
    }

    private fun safeDestroySTT() {
        try {
            speechRecognizer?.destroy()
            Log.d("SpeechService", "safeDestroySTT: SpeechRecognizer destroy 완료")
        } catch (e: Exception) {
            Log.e("SpeechService", "safeDestroySTT: destroy 중 예외", e)
        }
        speechRecognizer = null
    }

    // 서비스 종료/재시작 로그 강화
    fun onServiceCreate() {
        Log.d("SpeechService", "[라이프사이클] onCreate 호출")
    }
    fun onServiceDestroy() {
        Log.d("SpeechService", "[라이프사이클] onDestroy 호출")
    }
}