package com.example.hanbangreportnative

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Bundle
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import android.util.Log
import android.location.Geocoder
import android.widget.ImageView
import java.util.Random
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import android.app.PendingIntent

class FloatingBallService : Service() {
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("FloatingBallService", "onCreate 진입")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            Log.e("FloatingBallService", "오버레이 권한 없음, 서비스 중단")
            stopSelf()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("FloatingBallService", "노티피케이션 권한 없음, 서비스 중단")
            stopSelf()
            return
        }
        Log.d("FloatingBallService", "권한 모두 통과, ForegroundService 준비")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("FloatingBallService", "ForegroundService 시작")
            val channelId = "floating_ball_channel"
            val channelName = "플로팅 볼 서비스"
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // 알림 채널 생성
            if (nm.getNotificationChannel(channelId) == null) {
                Log.d("FloatingBallService", "ForegroundService 알림 채널 생성")
                val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
                channel.enableVibration(true)
                channel.enableLights(true)
                channel.setShowBadge(true)
                channel.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, null)
                nm.createNotificationChannel(channel)
            } else {
                Log.d("FloatingBallService", "ForegroundService 알림 채널 이미 존재")
            }
            
            try {
                val notification: Notification = NotificationCompat.Builder(this, channelId)
                    .setContentTitle("플로팅 볼 동작 중")
                    .setContentText("플로팅 볼이 화면에 표시됩니다.")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                    .setVibrate(longArrayOf(0, 250, 250, 250))
                    .build()
                
                Log.d("FloatingBallService", "ForegroundService 알림 생성 완료, startForeground 호출")
                startForeground(1, notification)
                Log.d("FloatingBallService", "ForegroundService 시작 완료")
            } catch (e: Exception) {
                Log.e("FloatingBallService", "ForegroundService 알림 생성 실패: ${e.message}", e)
            }
        }
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Log.d("FloatingBallService", "addFloatingBall() 호출")
        addFloatingBall()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun addFloatingBall() {
        Log.d("FloatingBallService", "addFloatingBall 진입")
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.layout_floating_ball, null)

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val viewWidth = 100 * displayMetrics.density // layout_floating_ball.xml의 배경 width와 동일하게 맞춤

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        // 최초 생성 시 화면 우측에 붙도록 x 좌표 설정
        params.x = (screenWidth - viewWidth).toInt()
        params.y = 300

        // 드래그 및 마그네틱 개선 (ConstraintLayout 전체에 적용)
        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        // 마그네틱: 오직 오른쪽 끝에만 이동 (padding 별도 보정 불필요)
                        val screenWidth = resources.displayMetrics.widthPixels
                        val viewWidth = floatingView?.width ?: 0
                        params.x = screenWidth - viewWidth
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                }
                return false
            }
        })

        // 앱으로 돌아가기 버튼
        floatingView?.findViewById<ImageButton>(R.id.btn_return)?.setOnClickListener {
            val intent = Intent(this, ReportListActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("from_floating_ball", true)
            startActivity(intent)
            stopSelf()
        }
        // 앱 종료 버튼
        floatingView?.findViewById<ImageButton>(R.id.btn_close)?.setOnClickListener {
            showNotification("앱을 종료합니다.", false, 2001, false)
            stopSelf()
            android.os.Process.killProcess(android.os.Process.myPid())
        }
        // 임시 신고 버튼
        floatingView?.findViewById<ImageButton>(R.id.btn_report)?.setOnClickListener {
            saveReportData()
        }

        try {
            windowManager.addView(floatingView, params)
            Log.d("FloatingBallService", "floatingView addView 성공")
            
            // 플로팅 볼 생성 성공 알림
            showNotification("플로팅 볼이 활성화되었습니다.", false, 1002, false)

            // addView 후 실제 width를 알 수 있으므로, post로 위치 재조정
            floatingView?.post {
                val screenWidth = resources.displayMetrics.widthPixels
                val viewWidth = floatingView?.width ?: 0
                params.x = screenWidth - viewWidth
                windowManager.updateViewLayout(floatingView, params)
            }
        } catch (e: Exception) {
            Log.e("FloatingBallService", "floatingView addView 실패: ${e.message}", e)
        }
    }

    private fun saveReportData() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            showNotification("위치 권한이 필요합니다.", false, 2003, false)
            return
        }
        // FusedLocationProviderClient로 위치 요청
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location == null) {
                    showNotification("위치 정보를 가져올 수 없습니다.", false, 2004, false)
                    return@addOnSuccessListener
                }
                val lat = location.latitude
                val lng = location.longitude
                val time = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault()).format(Date())
                val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                // 주소 변환(Geocoder)
                var address = "위도: $lat, 경도: $lng"
                try {
                    val geocoder = Geocoder(this@FloatingBallService, Locale.KOREA)
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        address = addresses[0].getAddressLine(0)
                    }
                } catch (e: Exception) {
                    Log.e("FloatingBallService", "주소 변환 실패: "+e.message)
                }
                // 고유 ID 생성 (날짜 + 5자리 순번)
                val existingList = ReportDataStore.loadList(this@FloatingBallService)
                val todayReports = existingList.filter { it.id.startsWith(date) }
                val nextNumber = (todayReports.size + 1).toString().padStart(5, '0')
                val id = "$date-$nextNumber"
                // 위반 타입 랜덤 선택
                val violationTypes = listOf("교통 위반", "신호 위반")
                val violationType = violationTypes[Random().nextInt(violationTypes.size)]
                // 새로운 신고 데이터 생성
                val reportData = ReportData(
                    id = id,
                    type = "신고 대기",
                    selected = false,
                    violationType = violationType,
                    datetime = time,
                    location = address,
                    thumbnail = "ex_art1"
                )
                // 데이터 저장
                val success = ReportDataStore.addReport(this@FloatingBallService, reportData)
                if (success) {
                    showNotification("신고 데이터가 저장되었습니다.", false, 1001, true)
                } else {
                    showNotification("리스트가 가득찼습니다. 기존 리스트를 삭제해주세요.", false, 2006, false)
                }
            }
            .addOnFailureListener { e ->
                showNotification("위치 정보를 가져오는 데 실패했습니다.", false, 2007, false)
                Log.e("FloatingBallService", "위치 요청 실패: "+e.message)
                }
            }

    private fun showNotification(message: String, isService: Boolean = false, notificationId: Int? = null, isReportData: Boolean = false) {
        Log.d("FloatingBallService", "showNotification 호출: message=$message, isService=$isService, isReportData=$isReportData")
        
        val channelId = if (isService) "floating_ball_channel" else "floating_ball_notify_channel"
        val channelName = if (isService) "플로팅 볼 서비스" else "플로팅 볼 임시 알림"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 알림 채널 생성
        if (nm.getNotificationChannel(channelId) == null) {
            Log.d("FloatingBallService", "알림 채널 생성: $channelId")
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            channel.enableVibration(true)
            channel.enableLights(true)
            channel.setShowBadge(true)
            nm.createNotificationChannel(channel)
        }
        
        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(if (isService) "플로팅 볼 동작 중" else "알림")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        
        // 신고 데이터 저장 알림인 경우에만 ReportListActivity로 이동하는 PendingIntent 설정
        if (isReportData) {
            Log.d("FloatingBallService", "신고 데이터 알림 - PendingIntent 설정")
            val intent = Intent(this, ReportListActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("from_floating_ball", true)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(pendingIntent)
        }
        
        if (isService) {
            builder.setOngoing(true)
            builder.setCategory(NotificationCompat.CATEGORY_SERVICE)
        } else {
            builder.setAutoCancel(true)
            builder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
        }
        
        val notification = builder.build()
        val id = notificationId ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        
        try {
            nm.notify(id, notification)
            Log.d("FloatingBallService", "알림 전송 성공: id=$id, message=$message")
        } catch (e: Exception) {
            Log.e("FloatingBallService", "알림 전송 실패: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("FloatingBallService", "onDestroy 호출")
        if (floatingView != null) windowManager.removeView(floatingView)
    }
}