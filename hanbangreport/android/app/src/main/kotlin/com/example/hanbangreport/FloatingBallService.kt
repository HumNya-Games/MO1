package com.example.hanbangreport

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast

class FloatingBallService : Service() {

    companion object {
        private const val CHANNEL_ID = "floating_ball_channel"
        private const val NOTIFICATION_ID = 1
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    override fun onCreate() {
        super.onCreate()

        Log.d("FloatingBallService", "onCreate 호출")

        createNotificationChannel()
        startForegroundServiceWithNotification()
        setupFloatingBall()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Ball Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
            Log.d("FloatingBallService", "Notification 채널 생성 완료")
        }
    }

    private fun startForegroundServiceWithNotification() {
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("플로팅볼 실행 중")
                .setContentText("플로팅볼이 화면에 표시됩니다.")
                .setSmallIcon(R.drawable.ic_temp_action)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("플로팅볼 실행 중")
                .setContentText("플로팅볼이 화면에 표시됩니다.")
                .setSmallIcon(R.drawable.ic_temp_action)
                .build()
        }

        Log.d("FloatingBallService", "startForeground 호출 직전")
        startForeground(NOTIFICATION_ID, notification)
        Log.d("FloatingBallService", "startForeground 호출 완료")
    }

    private fun setupFloatingBall() {
        Log.d("FloatingBallService", "setupFloatingBall 호출")

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.layout_floating_ball, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        // 앱 복귀 버튼 → 기존 앱 인스턴스의 report_list로 이동
        floatingView?.findViewById<ImageView>(R.id.btn_return)?.setOnClickListener {
            val intent = Intent("com.example.hanbangreport.SHOW_REPORT_LIST")
            sendBroadcast(intent)

            removeFloatingViewAndStop()
        }

        // 종료 버튼 → 플로팅볼만 종료
        floatingView?.findViewById<ImageView>(R.id.btn_close)?.setOnClickListener {
            Toast.makeText(this, "플로팅볼 종료", Toast.LENGTH_SHORT).show()
            removeFloatingViewAndStop()
        }

        // 임시 액션 버튼 (버튼 id: btn_temp_action)
        floatingView?.findViewById<ImageView>(R.id.btn_temp_action)?.setOnClickListener {
            Log.d("FloatingBallService", "임시 액션 버튼 클릭됨")
            Toast.makeText(this, "임시 액션 실행!", Toast.LENGTH_SHORT).show()
        }

        windowManager?.addView(floatingView, params)
        Log.d("FloatingBallService", "플로팅 뷰 화면에 추가 완료")
    }

    private fun removeFloatingViewAndStop() {
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
            floatingView = null
        }
        stopSelf()
        Log.d("FloatingBallService", "플로팅뷰 제거 및 서비스 종료")
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingViewAndStop()
        Log.d("FloatingBallService", "서비스 종료")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
