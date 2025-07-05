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
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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

class FloatingBallService : Service() {
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

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
            if (nm.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
                nm.createNotificationChannel(channel)
            }
            val notification: Notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("플로팅 볼 동작 중")
                .setContentText("플로팅 볼이 화면에 표시됩니다.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build()
            startForeground(1, notification)
        }
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Log.d("FloatingBallService", "addFloatingBall() 호출")
        addFloatingBall()
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
            Toast.makeText(this, "앱을 종료합니다.", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "플로팅 볼 추가됨", Toast.LENGTH_SHORT).show()

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
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, object : LocationListener {
            override fun onLocationChanged(location: Location) {
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
                    Log.e("FloatingBallService", "주소 변환 실패: ${e.message}")
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
                    Toast.makeText(this@FloatingBallService, "신고 데이터가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@FloatingBallService, "리스트가 가득찼습니다. 기존 리스트를 삭제해주세요.", Toast.LENGTH_LONG).show()
                }
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("FloatingBallService", "onDestroy 호출")
        if (floatingView != null) windowManager.removeView(floatingView)
    }
}