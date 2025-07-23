package com.example.hanbangreportnative

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hanbangreportnative.databinding.ActivitySettingBinding

class SettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var vibrator: Vibrator

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_VIBRATION = "vibration_on"
        private const val KEY_VOLUME = "volume_level"

        // 임시 버전 정보
        private const val CURRENT_VERSION = "1.0.0"
        private const val LATEST_VERSION = "1.0.1"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavBar.setCurrentScreen(3)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        setupVersionInfo()
        setupClickableLinks()
        setupSoundSetting()
        setupVibrationSetting()
        setupDataResetButton()
    }

    private fun setupVersionInfo() {
        val currentAppVersion = getAppVersion()
        binding.versionInfoText.text = "현재 버전: $currentAppVersion"

        val isLatestVersion = CURRENT_VERSION == LATEST_VERSION

        if (isLatestVersion) {
            binding.versionButton.setBackgroundResource(R.drawable.version_bg)
            binding.versionStatusText.text = "최신 버전 입니다."
            binding.versionButton.setOnClickListener {
                Toast.makeText(this, "최신 버전입니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.versionButton.setBackgroundResource(R.drawable.version_bg_on)
            binding.versionStatusText.text = "업데이트하러 가기."
            binding.versionButton.setOnClickListener {
                openUrl("https://jibbbob.quv.kr/5")
            }
        }
    }

    private fun setupClickableLinks() {
        binding.announcementButton.setOnClickListener {
            openUrl("https://jibbbob.quv.kr/5")
        }
        binding.howToUseButton.setOnClickListener {
            openUrl("https://jibbbob.quv.kr/5")
        }
        binding.termsButton.setOnClickListener {
            openUrl("https://jibbbob.quv.kr/5")
        }
        binding.privacyPolicyButton.setOnClickListener {
            openUrl("https://jibbbob.quv.kr/5")
        }
    }

    private fun setupSoundSetting() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = prefs.getInt(KEY_VOLUME, 7) // 기본값 7

        binding.seekbarVolume.max = maxVolume
        binding.seekbarVolume.progress = currentVolume
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)

        binding.seekbarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                    playSampleSound() // 소리 변경 시 예시 음 출력
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.progress?.let {
                    prefs.edit().putInt(KEY_VOLUME, it).apply()
                }
            }
        })
    }

    private fun playSampleSound() {
        // 기본 알림 사운드 재생
        val notificationUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val mediaPlayer = MediaPlayer.create(this, notificationUri)
        mediaPlayer.start()

        mediaPlayer.setOnCompletionListener {
            it.release() // 재생 완료 후 리소스 해제
        }
    }

    private fun setupVibrationSetting() {
        val isVibrationOn = prefs.getBoolean(KEY_VIBRATION, true) // 기본값 ON
        binding.vibrationSwitch.isChecked = isVibrationOn
        binding.vibrationStatusText.text = if (isVibrationOn) "ON" else "OFF"

        binding.vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.vibrationStatusText.text = if (isChecked) "ON" else "OFF"
            prefs.edit().putBoolean(KEY_VIBRATION, isChecked).apply()

            if (isChecked) {
                vibrate() // 진동 발생
            }
        }
    }

    private fun vibrate() {
        // 500ms 동안 진동
        vibrator.vibrate(500)
    }

    private fun setupDataResetButton() {
        binding.btnResetData.setOnClickListener {
            // 신고 리스트 데이터 초기화
            ReportDataStore.clearAll(this)
            ReportDataStore.resetDeletedCount(this)

            // '다시 보지 않기' 상태 초기화
            val drivePrefs = getSharedPreferences("drive_prefs", Context.MODE_PRIVATE)
            drivePrefs.edit().clear().apply()

            // 현재 화면의 설정값 초기화
            prefs.edit().clear().apply()

            // UI를 초기화된 값으로 즉시 갱신
            setupSoundSetting()
            setupVibrationSetting()

            Toast.makeText(this, "데이터가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAppVersion(): String {
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName ?: "1.0.0" // versionName이 null일 경우 기본값 반환
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            "1.0.0" // 예외 발생 시 기본값 반환
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            // 유효하지 않은 URL 또는 처리할 앱이 없는 경우를 대비
            Toast.makeText(this, "페이지를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}
