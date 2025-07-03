package com.example.hanbangreportnative

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class ReportListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_list)
        val prefs = getSharedPreferences("report_data", Context.MODE_PRIVATE)
        val count = prefs.getInt("count", 0)
        val data = mutableListOf<String>()
        for (i in 0 until count) {
            prefs.getString("report_data_$i", null)?.let { data.add(it) }
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_2, android.R.id.text1, data)
        val listView = findViewById<ListView>(R.id.report_list_view)
        listView.adapter = adapter
        findViewById<BottomNavBar>(R.id.bottom_nav_bar).setCurrentScreen(1)
    }
}
