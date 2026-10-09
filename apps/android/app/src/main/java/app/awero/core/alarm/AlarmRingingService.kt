package app.awero.core.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

class AlarmRingingService : Service() {
    private var player: MediaPlayer? = null
    private var fallbackTone: ToneGenerator? = null
    private var vibrator: Vibrator? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val fallbackToneLoop = object : Runnable {
        override fun run() {
            val tone = fallbackTone ?: return
            if (tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, FALLBACK_TONE_DURATION_MS)) {
                mainHandler.postDelayed(this, FALLBACK_TONE_INTERVAL_MS)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRinging()
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getStringExtra(AlarmScheduler.EXTRA_ID)
        if (alarmId == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val version = intent.getIntExtra(AlarmScheduler.EXTRA_VERSION, -1)
        val scheduledAt = intent.getLongExtra(AlarmScheduler.EXTRA_AT, System.currentTimeMillis())
        val test = intent.getBooleanExtra(AlarmScheduler.EXTRA_TEST, false)
        val notification = AlarmNotificationManager.build(this, alarmId, version, scheduledAt, test)
        ServiceCompat.startForeground(
            this,
            AlarmNotificationManager.NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
        startRinging()
        return START_NOT_STICKY
    }

    private fun startRinging() {
        stopRinging()
        startVibration()
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: Settings.System.DEFAULT_ALARM_ALERT_URI
            val mediaPlayer = MediaPlayer()
            player = mediaPlayer
            mediaPlayer.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmRingingService, uri)
                isLooping = true
                setOnPreparedListener { preparedPlayer ->
                    runCatching { preparedPlayer.start() }
                        .onFailure { startFallbackTone() }
                }
                setOnErrorListener { failedPlayer, _, _ ->
                    releasePlayer(failedPlayer)
                    startFallbackTone()
                    true
                }
                prepareAsync()
            }
        } catch (_: Exception) {
            releasePlayer()
            startFallbackTone()
        }
    }

    private fun startVibration() {
        runCatching {
            val deviceVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                getSystemService(Vibrator::class.java)
            }
            vibrator = deviceVibrator
            deviceVibrator.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN_MS, 0))
        }
    }

    private fun startFallbackTone() {
        releasePlayer()
        if (fallbackTone != null) return
        runCatching {
            fallbackTone = ToneGenerator(AudioManager.STREAM_ALARM, FALLBACK_TONE_VOLUME)
            mainHandler.post(fallbackToneLoop)
        }
    }

    private fun releasePlayer(target: MediaPlayer? = player) {
        target?.runCatching {
            setOnPreparedListener(null)
            setOnErrorListener(null)
            if (isPlaying) stop()
            release()
        }
        if (player === target) player = null
    }

    private fun stopRinging() {
        releasePlayer()
        mainHandler.removeCallbacks(fallbackToneLoop)
        fallbackTone?.runCatching {
            stopTone()
            release()
        }
        fallbackTone = null
        vibrator?.cancel()
        vibrator = null
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val ACTION_START = "app.awero.action.START_RINGING"
        private const val ACTION_STOP = "app.awero.action.STOP_RINGING"
        private const val FALLBACK_TONE_VOLUME = 100
        private const val FALLBACK_TONE_DURATION_MS = 900
        private const val FALLBACK_TONE_INTERVAL_MS = 1_300L
        private val VIBRATION_PATTERN_MS = longArrayOf(0, 500, 500)

        fun start(context: Context, alarmId: String, version: Int, scheduledAt: Long, test: Boolean) {
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                action = ACTION_START
                putExtra(AlarmScheduler.EXTRA_ID, alarmId)
                putExtra(AlarmScheduler.EXTRA_VERSION, version)
                putExtra(AlarmScheduler.EXTRA_AT, scheduledAt)
                putExtra(AlarmScheduler.EXTRA_TEST, test)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmRingingService::class.java))
        }
    }
}
