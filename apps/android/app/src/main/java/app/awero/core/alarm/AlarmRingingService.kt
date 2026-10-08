package app.awero.core.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

class AlarmRingingService : Service() {
    private var player: MediaPlayer? = null

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
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: Settings.System.DEFAULT_ALARM_ALERT_URI
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmRingingService, uri)
                isLooping = true
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, _, _ ->
                    stopRinging()
                    stopSelf()
                    true
                }
                prepareAsync()
            }
        } catch (_: Exception) {
            stopRinging()
            stopSelf()
        }
    }

    private fun stopRinging() {
        player?.runCatching {
            if (isPlaying) stop()
            release()
        }
        player = null
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val ACTION_START = "app.awero.action.START_RINGING"
        private const val ACTION_STOP = "app.awero.action.STOP_RINGING"

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
