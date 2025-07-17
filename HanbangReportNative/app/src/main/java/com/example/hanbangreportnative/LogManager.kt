package com.example.hanbangreportnative

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object LogManager {
    private const val PREFS_NAME = "log_settings"
    private const val KEY_LOG_SIMPLIFIED = "log_simplified"
    
    // 간소화할 로그 패턴 리스트
    private val SIMPLIFIED_LOG_PATTERNS = listOf(
        "[VOSK][REPORT] 노이즈/무음 감지 (민감도 기준 미달), 타이머 유지",
        "[VOSK][REPORT][onPartialResult] recognizedText='', hypothesis=",
        "[VOSK][WAKEWORD][onPartialResult] recognizedText='', hypothesis=",
        "[VOSK][WAKEWORD] 웨이크워드 인식 실패 (Partial)",
        "[VOSK][REPORT] 노이즈/무음 감지 (민감도 기준 미달), 타이머 유지"
    )
    
    private var isLogSimplified = false
    private lateinit var prefs: SharedPreferences
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isLogSimplified = prefs.getBoolean(KEY_LOG_SIMPLIFIED, false)
    }
    
    fun setLogSimplified(enabled: Boolean) {
        isLogSimplified = enabled
        prefs.edit().putBoolean(KEY_LOG_SIMPLIFIED, enabled).apply()
    }
    
    fun isLogSimplified(): Boolean = isLogSimplified
    
    fun d(tag: String, message: String) {
        if (shouldLog(message)) {
            Log.d(tag, message)
        }
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog(message)) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }
    
    fun i(tag: String, message: String) {
        if (shouldLog(message)) {
            Log.i(tag, message)
        }
    }
    
    fun w(tag: String, message: String) {
        if (shouldLog(message)) {
            Log.w(tag, message)
        }
    }
    
    private fun shouldLog(message: String): Boolean {
        if (!isLogSimplified) return true
        
        // 간소화 모드가 활성화된 경우 특정 패턴의 로그는 출력하지 않음
        return !SIMPLIFIED_LOG_PATTERNS.any { pattern ->
            message.contains(pattern)
        }
    }
    
    // 간소화할 로그 패턴 리스트 반환 (설정 화면에서 표시용)
    fun getSimplifiedLogPatterns(): List<String> = SIMPLIFIED_LOG_PATTERNS
} 