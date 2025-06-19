package com.example.hanbangreport

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private val CHANNEL = "com.hanbangreport/floating_ball"
    private var methodChannel: MethodChannel? = null

    // BroadcastReceiver 등록 (앱 외부 이벤트 수신)
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.hanbangreport.SHOW_REPORT_LIST") {
                Log.d("MainActivity", "BroadcastReceiver 수신 → /report_list 로 이동")
                methodChannel?.let {
                    it.invokeMethod("navigateToRoute", "/report_list")
                } ?: Log.w("MainActivity", "methodChannel이 초기화되지 않았습니다!")
            }
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)

        methodChannel?.setMethodCallHandler { call, result ->
            when (call.method) {
                "startFloatingBall" -> {
                    try {
                        startFloatingBallService()
                        result.success(null)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "플로팅볼 서비스 시작 실패", e)
                        result.error("SERVICE_START_FAILED", e.message, null)
                    }
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
        runOnUiThread {
            moveTaskToBack(true)  // 앱 백그라운드 이동, UI 스레드에서 호출
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate 호출됨")
    }

    override fun onResume() {
        super.onResume()
        try {
            val filter = IntentFilter("com.example.hanbangreport.SHOW_REPORT_LIST")
            registerReceiver(broadcastReceiver, filter)
            Log.d("MainActivity", "BroadcastReceiver 등록 완료")
        } catch (e: Exception) {
            Log.e("MainActivity", "BroadcastReceiver 등록 중 오류 발생", e)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(broadcastReceiver)
            Log.d("MainActivity", "BroadcastReceiver 해제 완료")
        } catch (e: IllegalArgumentException) {
            Log.w("MainActivity", "BroadcastReceiver가 이미 해제된 상태입니다.")
        }
    }

    // onNewIntent() 사용하지 않음, BroadcastReceiver가 이벤트 처리
}
