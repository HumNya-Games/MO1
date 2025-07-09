package com.example.hanbangreportnative

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast

object ToastUtils {
    fun showCustomToast(context: Context, message: String) {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.custom_toast, null)
        
        val textView = layout.findViewById<TextView>(R.id.toast_text)
        textView.text = message
        
        val toast = Toast(context)
        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, getTopOffset(context))
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }
    
    private fun getTopOffset(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        // 화면 상단에서 약 27% 위치
        return (screenHeight * 0.27).toInt()
    }
} 