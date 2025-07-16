package com.example.hanbangreportnative

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ReportDataStore {
    private const val PREFS_NAME = "report_list"
    private const val KEY_LIST = "report_list_json"
    private const val KEY_DELETED_COUNT = "report_deleted_count"
    private const val MAX_SIZE = 10000

    private val gson = Gson()

    fun loadList(context: Context): MutableList<ReportData> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_LIST, null)
        return if (json.isNullOrEmpty()) mutableListOf() else gson.fromJson(json, object : TypeToken<MutableList<ReportData>>(){}.type)
    }

    fun saveList(context: Context, list: List<ReportData>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LIST, gson.toJson(list)).apply()
    }

    fun addReport(context: Context, report: ReportData, callback: (String?) -> Unit) {
        val list = loadList(context)
        if (list.size >= MAX_SIZE) {
            callback(null)
            return
        }
        list.add(0, report)
        saveList(context, list)
        callback(report.id)
    }

    fun updateList(context: Context, list: List<ReportData>) {
        saveList(context, list)
    }

    fun clearAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun getDeletedCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_DELETED_COUNT, 0)
    }

    fun increaseDeletedCount(context: Context, count: Int = 1) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_DELETED_COUNT, 0)
        prefs.edit().putInt(KEY_DELETED_COUNT, current + count).apply()
    }

    fun resetDeletedCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_DELETED_COUNT, 0).apply()
    }

    fun updateReportViolationType(context: Context, id: String, newViolationType: String, callback: (Boolean) -> Unit) {
        val list = loadList(context)
        val idx = list.indexOfFirst { it.id == id }
        if (idx != -1) {
            list[idx].violationType = newViolationType
            saveList(context, list)
            callback(true)
        } else {
            callback(false)
        }
    }
} 