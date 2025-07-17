package com.example.hanbangreportnative

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import android.view.View
import kotlinx.coroutines.*

class LogoActivity : Activity() {
    companion object {
        // 추후 다양한 체크(버전, 네트워크 등) 확장 가능
        const val DELAY_MILLIS = 3000L // 기존 딜레이는 제거
    }
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingText: TextView
    private lateinit var retryButton: View
    private var copyJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logo)
        progressBar = findViewById(R.id.progressBar)
        loadingText = findViewById(R.id.loadingText)
        retryButton = findViewById(R.id.retryButton)
        retryButton.setOnClickListener {
            startLoadingChecks()
        }
        startLoadingChecks()
    }

    private fun startLoadingChecks() {
        progressBar.visibility = View.VISIBLE
        loadingText.text = "음성 인식 준비 중입니다. 잠시만 기다려주세요..."
        retryButton.visibility = View.GONE
        copyJob?.cancel()
        copyJob = CoroutineScope(Dispatchers.IO).launch {
            val start = System.currentTimeMillis()
            var errorMsg: String? = null
            val result = runCatching {
                val dummyCallback = object : SpeechService.Callback {
                    override fun onReportTrigger() {}
                    override fun onReportContentResult(content: String) {}
                    override fun onAppExit() {}
                    override fun onError(error: String) { errorMsg = error }
                }
                val speechService = SpeechService(this@LogoActivity, dummyCallback)
                // 모델 복사/압축해제 및 검증: initVoskModel()을 직접 호출
                val initModelMethod = speechService.javaClass.getDeclaredMethod("initVoskModel")
                initModelMethod.isAccessible = true
                initModelMethod.invoke(speechService)
            }
            val end = System.currentTimeMillis()
            withContext(Dispatchers.Main) {
                if (result.isSuccess && errorMsg == null) {
                    loadingText.text = "준비 완료!"
                    progressBar.visibility = View.GONE
                    delay(500)
                    startActivity(Intent(this@LogoActivity, TitleActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                } else {
                    loadingText.text = errorMsg ?: "음성 인식 준비에 실패했습니다. 다시 시도해 주세요."
                    progressBar.visibility = View.GONE
                    retryButton.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroy() {
        copyJob?.cancel()
        super.onDestroy()
    }
}