package com.example.hanbangreportnative

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

/**
 * 신고 음성 보정 데이터를 관리하는 클래스
 * 최대 100개까지 저장하며, FIFO 방식으로 관리
 */
object CorrectionDataStore {
    private const val PREFS_NAME = "speech_settings"
    private const val KEY_CORRECTION_DATA = "correction_data_list"
    private const val MAX_CORRECTION_DATA = 100
    
    private val gson = Gson()
    
    /**
     * 보정 데이터 항목
     */
    data class CorrectionData(
        val text: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * 보정 데이터 리스트 로드
     */
    fun loadCorrectionData(context: Context): MutableList<CorrectionData> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CORRECTION_DATA, null)
        
        return if (json.isNullOrBlank()) {
            mutableListOf()
        } else {
            try {
                val type = object : TypeToken<MutableList<CorrectionData>>() {}.type
                gson.fromJson(json, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }
        }
    }
    
    /**
     * 보정 데이터 리스트 저장
     */
    private fun saveCorrectionData(context: Context, dataList: List<CorrectionData>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(dataList)
        prefs.edit().putString(KEY_CORRECTION_DATA, json).apply()
    }
    
    /**
     * 새로운 보정 데이터 추가
     */
    fun addCorrectionData(context: Context, text: String) {
        val dataList = loadCorrectionData(context).toMutableList()
        
        // 중복 체크 (텍스트만 비교)
        if (dataList.any { it.text == text }) {
            return
        }
        
        // 새 데이터 추가
        val newData = CorrectionData(text)
        dataList.add(newData)
        
        // 최대 개수 제한 (FIFO)
        if (dataList.size > MAX_CORRECTION_DATA) {
            // 가장 오래된 데이터부터 삭제
            dataList.sortBy { it.timestamp }
            while (dataList.size > MAX_CORRECTION_DATA) {
                dataList.removeAt(0)
            }
        }
        
        saveCorrectionData(context, dataList)
    }
    
    /**
     * 모든 보정 데이터 삭제
     */
    fun clearAllCorrectionData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CORRECTION_DATA).apply()
    }
    
    /**
     * 보정 데이터 리스트 반환
     */
    fun getCorrectionDataList(context: Context): List<CorrectionData> {
        return loadCorrectionData(context)
    }
    
    /**
     * 보정 데이터 텍스트만 반환
     */
    fun getCorrectionTextList(context: Context): List<String> {
        return loadCorrectionData(context).map { it.text }
    }
    
    /**
     * 특정 텍스트가 보정 데이터에 포함되어 있는지 확인
     */
    fun containsCorrectionData(context: Context, text: String): Boolean {
        val dataList = loadCorrectionData(context)
        return dataList.any { it.text == text }
    }
    
    /**
     * 보정 데이터 개수 반환
     */
    fun getCorrectionDataCount(context: Context): Int {
        return loadCorrectionData(context).size
    }
}
