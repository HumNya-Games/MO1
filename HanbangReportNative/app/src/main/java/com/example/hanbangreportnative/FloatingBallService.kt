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

// SpeechService 관련 import 추가

class FloatingBallService : Service() {
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var speechService: SpeechService? = null
    private var lastReportId: String? = null // 최근 저장된 신고 id 보관

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        Log.d("FloatingBallService", "onCreate 진입 (서비스 생성/재시작)")
        super.onCreate()
        Log.d("FloatingBallService", "onCreate 진입")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            Log.e("FloatingBallService", "오버레이 권한 없음, 서비스 중단(실제 종료는 하지 않음, 안내만)")
            // stopSelf() // 서비스 종료 금지, 안내만
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("FloatingBallService", "노티피케이션 권한 없음, 서비스 중단(실제 종료는 하지 않음, 안내만)")
            // stopSelf() // 서비스 종료 금지, 안내만
            return
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "마이크 권한이 필요합니다. 설정에서 허용해 주세요.", Toast.LENGTH_LONG).show()
            Log.e("FloatingBallService", "마이크 권한 없음, 서비스 중단(실제 종료는 하지 않음, 안내만)")
            // stopSelf() // 서비스 종료 금지, 안내만
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

        // SpeechService 초기화 및 플로팅 볼 활성화 상태 설정
        speechService = SpeechService(this, object : SpeechService.Callback {
            override fun onReportTrigger() {
                // "신고" 트리거 인식 시 임시 저장(알림/안내 없음), type은 "내용 없음"으로 고정
                saveReportData(voiceMode = true, voiceContent = "내용 없음", isFinal = false) { id ->
                    lastReportId = id
                    // speechService?.notifyReadyForContentSTT() // 리팩터링 후 불필요, 삭제
                }
            }
            override fun onReportContentResult(content: String) {
                // 음성 인식 결과(실제 텍스트 또는 "내용 없음")를 violationType에 저장
                // 직전 저장된 id의 신고 violationType만 갱신
                lastReportId?.let { id ->
                    ReportDataStore.updateReportViolationType(this@FloatingBallService, id, content) { updated ->
                        if (updated) {
                            showNotification("신고 내용이 저장되었습니다.", false, 1001, true)
                            // TTS 안내는 SpeechService에서만 한 번만 출력 (중복 방지)
                        } else {
                            showNotification("신고 데이터 violationType 갱신 실패", false, 2008, false)
                        }
                    }
                }
            }
            override fun onAppExit() {
                // "앱 종료" 음성 인식 시 앱 종료
                Log.d("FloatingBallService", "앱 종료 음성 인식됨")
                showNotification("앱을 종료합니다.", false, 2001, false)
                // 음성 서비스 종료
                speechService?.destroy()
                // 플로팅 볼 서비스 종료
                stopSelf()
                // 앱 완전 종료
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(0)
            }
            override fun onError(error: String) {
                Toast.makeText(this@FloatingBallService, error, Toast.LENGTH_SHORT).show()
            }
        })
        
        // 플로팅 볼 활성화 상태 설정 및 음성 인식 시작
        speechService?.setFloatingBallActive(true)
        speechService?.setAppForegroundState(false) // 플로팅 볼 서비스는 백그라운드에서 실행
        
        // 음성 인식 시작 전 상태 확인
        Log.d("FloatingBallService", "speechService?.start() 호출 직전")
        speechService?.start()
        Log.d("FloatingBallService", "speechService?.start() 호출 완료")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("FloatingBallService", "onStartCommand 진입 (서비스 시작/재시작)")
        super.onStartCommand(intent, flags, startId)
        return super.onStartCommand(intent, flags, startId)
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
            returnToApp()
        }
        // 앱 종료 버튼
        floatingView?.findViewById<ImageButton>(R.id.btn_close)?.setOnClickListener {
            showNotification("앱을 종료합니다.", false, 2001, false)
            // 음성 서비스 종료
            speechService?.destroy()
            // 플로팅 볼 서비스 종료
            stopSelf()
            // 앱 완전 종료
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(0)
        }
        // 임시 신고 버튼
        floatingView?.findViewById<ImageButton>(R.id.btn_report)?.setOnClickListener {
            // 음성 인식 시작
            startVoiceRecognition()
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

    /**
     * 음성 인식 시작
     */
    private fun startVoiceRecognition() {
        Log.d("FloatingBallService", "음성 인식 시작")
        showNotification("음성 인식이 시작되었습니다. '신고'라고 말씀해주세요.", false, 1003, false)
        speechService?.start()
    }

    /**
     * @param isFinal true: 신고 내용(type) 최종 저장 시(알림/안내 출력), false: 임시 저장(알림 없음)
     * @return 저장된 신고 id(고유번호), 실패 시 null
     */
    private fun saveReportData(
        voiceMode: Boolean = false,
        voiceContent: String? = null,
        isFinal: Boolean = false,
        onSaved: ((String?) -> Unit)? = null
    ) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            showNotification("위치 권한이 필요합니다.", false, 2003, false)
            onSaved?.invoke(null)
            return
        }
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location == null) {
                    showNotification("위치 정보를 가져올 수 없습니다.", false, 2004, false)
                    onSaved?.invoke(null)
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
                // type 값 결정
                val typeValue = "신고 대기" // 항상 "신고 대기"로 고정
                val violationTypeValue = voiceContent ?: "내용 없음" // 신고 내용 또는 "내용 없음"
                // 새로운 신고 데이터 생성
                val reportData = ReportData(
                    id = id,
                    type = typeValue,
                    selected = false,
                    violationType = violationTypeValue,
                    datetime = time,
                    location = address,
                    thumbnail = "ex_art1"
                )
                // 데이터 저장 (콜백 기반으로 변경)
                ReportDataStore.addReport(this@FloatingBallService, reportData) { savedId ->
                    if (savedId != null && isFinal) {
                    showNotification("신고 데이터가 저장되었습니다.", false, 1001, true)
                    }
                    if (savedId == null) {
                    showNotification("리스트가 가득찼습니다. 기존 리스트를 삭제해주세요.", false, 2006, false)
                    }
                    onSaved?.invoke(savedId)
                }
            }
            .addOnFailureListener { e ->
                showNotification("위치 정보를 가져오는 데 실패했습니다.", false, 2007, false)
                Log.e("FloatingBallService", "위치 요청 실패: "+e.message)
                onSaved?.invoke(null)
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
        
        // '신고 데이터가 저장되었습니다.'는 모두 '신고 내용이 저장되었습니다.'로 치환
        val fixedMessage = if (message == "신고 데이터가 저장되었습니다.") "신고 내용이 저장되었습니다." else message
        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(if (isService) "플로팅 볼 동작 중" else "알림")
            .setContentText(fixedMessage)
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

    // 플로팅 볼 클릭 시 앱으로 복귀
    private fun returnToApp() {
        Log.d("FloatingBallService", "앱으로 복귀 시작")
        
        // 1. SpeechService 완전 종료
        speechService?.setAppForegroundState(true) // 앱 포그라운드 상태로 설정하여 음성 인식 종료
        speechService?.stop() // 음성 인식 서비스 중지
        speechService?.destroy() // 음성 인식 서비스 완전 정리
        speechService = null
        
        // 2. 플로팅 볼 제거
        removeFloatingBall()
        
        // 3. ReportListActivity로 복귀
        val intent = Intent(this, ReportListActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_floating_ball", true)
        }
        startActivity(intent)
        
        // 4. 서비스 종료
        stopSelf()
        
        Log.d("FloatingBallService", "앱으로 복귀 완료")
    }

    // 플로팅 볼 제거 함수
    private fun removeFloatingBall() {
        try {
            if (floatingView != null) {
                windowManager.removeView(floatingView)
                floatingView = null
                Log.d("FloatingBallService", "플로팅 볼이 정상적으로 제거되었습니다.")
            }
        } catch (e: Exception) {
            Log.e("FloatingBallService", "플로팅 볼 제거 중 오류: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        Log.d("FloatingBallService", "onDestroy 진입 (서비스 종료)")
        super.onDestroy()
        if (floatingView != null) windowManager.removeView(floatingView)
        // 서비스 종료 시 음성 인식 중단
        speechService?.stop()
        // 플로팅 볼 서비스 완전 종료
        stopSelf()
    }
}