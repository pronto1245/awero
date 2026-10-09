package app.awero.core.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.AudioManager
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
    private data class RingKey(val alarmId: String, val test: Boolean)

    private data class RingSession(
        val key: RingKey,
        var version: Int,
        var scheduledAt: Long,
        var player: MediaPlayer? = null,
        var fallbackTone: ToneGenerator? = null,
        var fallbackLoop: Runnable? = null
    )

    private val rings = linkedMapOf<RingKey, RingSession>()
    private var foregroundKey: RingKey? = null
    private var vibrator: Vibrator? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getStringExtra(AlarmScheduler.EXTRA_ID)
        val test = intent?.getBooleanExtra(AlarmScheduler.EXTRA_TEST, false) ?: false
        if (intent?.action == ACTION_STOP) {
            if (alarmId != null) stopRinging(RingKey(alarmId, test))
            if (rings.isEmpty()) stopSelf(startId)
            return START_NOT_STICKY
        }
        if (alarmId == null) {
            if (rings.isEmpty()) stopSelf(startId)
            return START_NOT_STICKY
        }

        val version = intent.getIntExtra(AlarmScheduler.EXTRA_VERSION, -1)
        val scheduledAt = intent.getLongExtra(AlarmScheduler.EXTRA_AT, System.currentTimeMillis())
        val key = RingKey(alarmId, test)
        val existing = rings[key]
        if (existing != null) {
            existing.version = version
            existing.scheduledAt = scheduledAt
            ensureForeground(key)
            return START_NOT_STICKY
        }

        val wasEmpty = rings.isEmpty()
        val session = RingSession(key, version, scheduledAt)
        rings[key] = session
        if (wasEmpty) startVibration()
        ensureForeground(key)
        startRinging(session)
        return START_NOT_STICKY
    }

    private fun ensureForeground(key: RingKey) {
        val session = rings[key] ?: return
        val notification = AlarmNotificationManager.build(
            this, key.alarmId, session.version, session.scheduledAt, key.test
        )
        val notificationId = AlarmNotificationManager.notificationId(key.alarmId, key.test)
        if (foregroundKey == null) {
            ServiceCompat.startForeground(
                this,
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
            foregroundKey = key
        } else if (foregroundKey == key) {
            ServiceCompat.startForeground(
                this,
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            getSystemService(NotificationManager::class.java)
                .notify(notificationId, notification)
        }
    }

    private fun startRinging(session: RingSession) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: Settings.System.DEFAULT_ALARM_ALERT_URI
            val mediaPlayer = MediaPlayer()
            session.player = mediaPlayer
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
                    if (rings[session.key] !== session) {
                        releasePlayer(session, preparedPlayer)
                    } else {
                        runCatching { preparedPlayer.start() }
                            .onFailure { startFallbackTone(session) }
                    }
                }
                setOnErrorListener { failedPlayer, _, _ ->
                    releasePlayer(session, failedPlayer)
                    startFallbackTone(session)
                    true
                }
                prepareAsync()
            }
        } catch (_: Exception) {
            releasePlayer(session)
            startFallbackTone(session)
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

    private fun startFallbackTone(session: RingSession) {
        if (rings[session.key] !== session || session.fallbackTone != null) return
        releasePlayer(session)
        runCatching {
            val handler = Handler(Looper.getMainLooper())
            val tone = ToneGenerator(AudioManager.STREAM_ALARM, FALLBACK_TONE_VOLUME)
            val loop = object : Runnable {
                override fun run() {
                    if (rings[session.key] !== session || session.fallbackTone !== tone) return
                    if (tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, FALLBACK_TONE_DURATION_MS)) {
                        handler.postDelayed(this, FALLBACK_TONE_INTERVAL_MS)
                    }
                }
            }
            session.fallbackTone = tone
            session.fallbackLoop = loop
            handler.post(loop)
        }
    }

    private fun releasePlayer(session: RingSession, target: MediaPlayer? = session.player) {
        target?.runCatching {
            setOnPreparedListener(null)
            setOnErrorListener(null)
            if (isPlaying) stop()
            release()
        }
        if (session.player === target) session.player = null
    }

    private fun stopRinging(key: RingKey) {
        val session = rings.remove(key) ?: return
        releasePlayer(session)
        session.fallbackLoop?.let { Handler(Looper.getMainLooper()).removeCallbacks(it) }
        session.fallbackTone?.runCatching {
            stopTone()
            release()
        }
        getSystemService(NotificationManager::class.java)
            .cancel(AlarmNotificationManager.notificationId(key.alarmId, key.test))

        if (foregroundKey == key) {
            foregroundKey = null
            val nextKey = rings.keys.firstOrNull()
            if (nextKey == null) {
                vibrator?.cancel()
                vibrator = null
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            } else {
                ensureForeground(nextKey)
            }
        } else if (rings.isEmpty()) {
            vibrator?.cancel()
            vibrator = null
            stopSelf()
        }
    }

    override fun onDestroy() {
        rings.values.toList().forEach { session ->
            releasePlayer(session)
            session.fallbackLoop?.let { Handler(Looper.getMainLooper()).removeCallbacks(it) }
            session.fallbackTone?.runCatching {
                stopTone()
                release()
            }
            getSystemService(NotificationManager::class.java).cancel(
                AlarmNotificationManager.notificationId(session.key.alarmId, session.key.test)
            )
        }
        rings.clear()
        vibrator?.cancel()
        vibrator = null
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

        fun stop(context: Context, alarmId: String, test: Boolean) {
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                action = ACTION_STOP
                putExtra(AlarmScheduler.EXTRA_ID, alarmId)
                putExtra(AlarmScheduler.EXTRA_TEST, test)
            }
            context.startService(intent)
        }
    }
}
