package com.example.hanbangreportnative

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlin.math.sqrt

/**
 * 오디오 레벨 측정을 위한 클래스
 * RMS(Root Mean Square) 방식으로 오디오 레벨을 측정
 */
class AudioLevelManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioLevelManager"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        
        // 안내 음성 길이 예측 상수 (한글 기준)
        private const val CHARS_PER_SECOND = 3.5f // 초당 약 3.5글자
        private const val MIN_DELAY_MS = 1000 // 최소 대기 시간 (1초)
        private const val EXTRA_DELAY_MS = 500 // 추가 대기 시간 (0.5초)
    }
    
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    
    /**
     * 오디오 레벨 측정 결과
     */
    data class AudioLevelResult(
        val rmsLevel: Double,
        val peakLevel: Double,
        val averageLevel: Double
    )
    
    /**
     * 오디오 레벨 측정 시작
     */
    fun startAudioLevelMeasurement(
        durationSeconds: Int,
        onProgress: (currentRms: Double) -> Unit,
        onComplete: (result: AudioLevelResult) -> Unit
    ) {
        if (isRecording) {
            Log.w(TAG, "이미 녹음 중입니다.")
            return
        }
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord 초기화 실패")
                return
            }
            
            isRecording = true
            recordingThread = Thread {
                measureAudioLevel(durationSeconds, onProgress, onComplete)
            }
            recordingThread?.start()
            
        } catch (e: Exception) {
            Log.e(TAG, "오디오 레벨 측정 시작 실패: ${e.message}")
        }
    }
    
    /**
     * 오디오 레벨 측정 중지
     */
    fun stopAudioLevelMeasurement() {
        isRecording = false
        recordingThread?.interrupt()
        recordingThread = null
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "오디오 레벨 측정 중지 실패: ${e.message}")
        }
    }
    
    /**
     * 실제 오디오 레벨 측정 수행
     */
    private fun measureAudioLevel(
        durationSeconds: Int,
        onProgress: (currentRms: Double) -> Unit,
        onComplete: (result: AudioLevelResult) -> Unit
    ) {
        val buffer = ShortArray(BUFFER_SIZE / 2)
        val samplesPerSecond = SAMPLE_RATE
        val totalSamples = samplesPerSecond * durationSeconds
        var samplesRead = 0
        
        val rmsValues = mutableListOf<Double>()
        val peakValues = mutableListOf<Double>()
        
        audioRecord?.startRecording()
        
        while (isRecording && samplesRead < totalSamples) {
            val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
            
            if (readSize > 0) {
                samplesRead += readSize
                
                // RMS 계산
                val rms = calculateRMS(buffer, readSize)
                rmsValues.add(rms)
                
                // 피크 값 계산
                val peak = calculatePeak(buffer, readSize)
                peakValues.add(peak)
                
                // 진행 상황 콜백
                onProgress(rms)
                
                // 100ms마다 진행 상황 업데이트
                if (samplesRead % (samplesPerSecond / 10) < readSize) {
                    Thread.sleep(10)
                }
            }
        }
        
        audioRecord?.stop()
        
        // 결과 계산
        val result = AudioLevelResult(
            rmsLevel = rmsValues.average(),
            peakLevel = peakValues.maxOrNull() ?: 0.0,
            averageLevel = rmsValues.average()
        )
        
        onComplete(result)
    }
    
    /**
     * RMS(Root Mean Square) 계산
     */
    private fun calculateRMS(buffer: ShortArray, size: Int): Double {
        var sum = 0.0
        for (i in 0 until size) {
            val sample = buffer[i].toDouble()
            sum += sample * sample
        }
        return sqrt(sum / size)
    }
    
    /**
     * 피크 값 계산
     */
    private fun calculatePeak(buffer: ShortArray, size: Int): Double {
        var peak = 0.0
        for (i in 0 until size) {
            val sample = buffer[i].toDouble()
            if (kotlin.math.abs(sample) > peak) {
                peak = kotlin.math.abs(sample)
            }
        }
        return peak
    }
    
    /**
     * 백그라운드 소음 측정 (5-10초)
     */
    fun measureBackgroundNoise(
        durationSeconds: Int = 10,
        onComplete: (backgroundRms: Double) -> Unit
    ) {
        startAudioLevelMeasurement(
            durationSeconds = durationSeconds,
            onProgress = { /* 진행 상황 무시 */ },
            onComplete = { result ->
                onComplete(result.rmsLevel)
            }
        )
    }
    
    /**
     * 음성 피크 측정 (명령어 테스트)
     */
    fun measureVoicePeak(
        durationSeconds: Int = 5,
        onComplete: (voicePeak: Double) -> Unit
    ) {
        startAudioLevelMeasurement(
            durationSeconds = durationSeconds,
            onProgress = { /* 진행 상황 무시 */ },
            onComplete = { result ->
                onComplete(result.peakLevel)
            }
        )
    }
    
    /**
     * 자동 임계값 계산
     */
    fun calculateAutoThresholds(
        backgroundRms: Double,
        voicePeak: Double
    ): Triple<Int, Int, Int> {
        val difference = voicePeak - backgroundRms
        
        // 중간 임계값 = 배경소음 평균 + (피크–배경)×0.4
        val mediumThreshold = (backgroundRms + difference * 0.4).toInt()
        
        // 높은 임계값 = 배경소음 평균 + (피크–배경)×0.7
        val highThreshold = (backgroundRms + difference * 0.7).toInt()
        
        // 마이크 민감도 = 중간 임계값과 비슷하게 설정
        val micSensitivity = mediumThreshold
        
        return Triple(
            micSensitivity.coerceIn(0, 5000),
            mediumThreshold.coerceIn(0, 5000),
            highThreshold.coerceIn(0, 5000)
        )
    }

    /**
     * 안내 음성 길이 예측 및 대기 시간 계산
     * @param text 안내 음성 텍스트
     * @return 대기해야 할 시간 (밀리초)
     */
    fun calculateVoiceDelay(text: String): Long {
        val estimatedDuration = (text.length / CHARS_PER_SECOND * 1000).toLong()
        return (estimatedDuration + EXTRA_DELAY_MS).coerceAtLeast(MIN_DELAY_MS.toLong())
    }
    
    /**
     * 안내 음성 재생 후 대기
     * @param text 안내 음성 텍스트
     * @param onComplete 대기 완료 후 실행할 콜백
     */
    fun waitForVoiceCompletion(text: String, onComplete: () -> Unit) {
        val delayMs = calculateVoiceDelay(text)
        Log.d(TAG, "안내 음성 대기: ${delayMs}ms (텍스트: $text)")
        
        Thread {
            try {
                Thread.sleep(delayMs)
                onComplete()
            } catch (e: InterruptedException) {
                Log.w(TAG, "음성 대기 중단됨")
            }
        }.start()
    }
    
    /**
     * 안내 음성 재생 후 대기 (비동기)
     * @param text 안내 음성 텍스트
     * @param onComplete 대기 완료 후 실행할 콜백
     */
    suspend fun waitForVoiceCompletionAsync(text: String, onComplete: () -> Unit) {
        val delayMs = calculateVoiceDelay(text)
        Log.d(TAG, "안내 음성 대기: ${delayMs}ms (텍스트: $text)")
        
        kotlinx.coroutines.delay(delayMs)
        onComplete()
    }
}
