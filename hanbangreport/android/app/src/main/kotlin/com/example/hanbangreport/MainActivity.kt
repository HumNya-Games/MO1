package com.example.hanbangreport

import android.content.Intent
import android.os.Build
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import com.example.hanbangreport.FloatingBallService

class MainActivity: FlutterActivity() {

    private val CHANNEL = "com.hanbangreport/floating_ball"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler {
            call, result ->
            when (call.method) {
                "startFloatingBall" -> {
                    startFloatingBallService()
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun startFloatingBallService() {
        val intent = Intent(this, FloatingBallService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        moveTaskToBack(true) // 앱을 백그라운드로 전환
    }
}