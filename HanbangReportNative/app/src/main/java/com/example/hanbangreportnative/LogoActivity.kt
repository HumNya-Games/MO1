package com.example.hanbangreportnative

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class LogoActivity : Activity() {
    companion object {
        const val DELAY_MILLIS = 3000L // 3초, 필요시 변경
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logo)
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, TitleActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }, DELAY_MILLIS)
    }
}