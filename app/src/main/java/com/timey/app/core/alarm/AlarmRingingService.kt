package com.timey.app.core.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.timey.app.MainActivity
import com.timey.app.R
import com.timey.app.data.preferences.TimeyPreferencesRepository
import com.timey.app.domain.model.AlarmSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/**
 * High-priority Foreground Ringing Service that guarantees audible ringing and vibration
 * when a study timer completes, even if the device is locked, muted, or in Doze mode.
 */
class AlarmRingingService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var autoStopJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_RINGING

        when (action) {
            ACTION_START_RINGING -> {
                val sessionTitle = intent?.getStringExtra(EXTRA_SESSION_TITLE) ?: "Focus Block"
                val durationMinutes = intent?.getIntExtra(EXTRA_DURATION_MINUTES, 25) ?: 25
                startAlarm(sessionTitle, durationMinutes)
            }
            ACTION_DISMISS_ALARM -> {
                dismissAlarm()
            }
        }

        return START_NOT_STICKY
    }

    private fun startAlarm(sessionTitle: String, durationMinutes: Int) {
        val notification = buildRingingNotification(sessionTitle, durationMinutes)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startVibration()
        playAlarmAudio()

        // Auto-stop ringing after 45 seconds to prevent permanent battery drain if unacknowledged
        autoStopJob?.cancel()
        autoStopJob = scope.launch {
            delay(45_000L)
            dismissAlarm()
        }
    }

    private fun playAlarmAudio() {
        scope.launch {
            try {
                val prefsRepo = TimeyPreferencesRepository(applicationContext)
                val settings = prefsRepo.settingsFlow.first()
                val volume = settings.alarmVolume.coerceIn(0.1f, 1.0f)
                val sound = settings.alarmSound

                if (sound.isSilent) return@launch

                stopAudio()

                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                            .build()
                    )
                    isLooping = true
                    setVolume(volume, volume)
                }

                var dataSourceSet = false

                if (sound == AlarmSound.CUSTOM_USER_AUDIO) {
                    val customFile = CustomAudioStorage.getStoredCustomAudioFile(applicationContext)
                    if (customFile != null && customFile.exists() && customFile.length() > 0) {
                        mp.setDataSource(customFile.absolutePath)
                        dataSourceSet = true
                    }
                }

                if (!dataSourceSet) {
                    val alarmUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                    if (alarmUri != null) {
                        mp.setDataSource(applicationContext, alarmUri)
                        dataSourceSet = true
                    }
                }

                if (dataSourceSet) {
                    mp.prepare()
                    mp.start()
                    mediaPlayer = mp
                } else {
                    // Fallback to synthesized acoustic bell
                    AlarmSoundSynthesizer.playAlarmSound(applicationContext, sound, volume)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start alarm audio: ${e.message}", e)
                try {
                    // Fail-safe: Synthesizer fallback
                    AlarmSoundSynthesizer.playAlarmSound(applicationContext, AlarmSound.ZEN_BELL, 0.9f)
                } catch (_: Exception) {}
            }
        }
    }

    private fun startVibration() {
        try {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    val timings = longArrayOf(0, 500, 200, 500, 200, 500, 1000)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createWaveform(timings, 0)) // Repeat continuously
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(timings, 0)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm vibration: ${e.message}")
        }
    }

    private fun buildRingingNotification(sessionTitle: String, durationMinutes: Int): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmRingingService::class.java).apply {
            action = ACTION_DISMISS_ALARM
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            NOTIFICATION_ID + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("⏱️ $sessionTitle Complete!")
            .setContentText("Finished $durationMinutes minutes of uninterrupted focus. Tap to dismiss.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(contentPendingIntent, true) // Heads-up display over lockscreen
            .addAction(R.drawable.ic_pause, "Dismiss Alarm", dismissPendingIntent)
            .build()
    }

    private fun dismissAlarm() {
        autoStopJob?.cancel()
        stopAudio()
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
        AlarmSoundSynthesizer.stop()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopAudio() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Timey:AlarmRingingWakeLock"
            )?.apply {
                acquire(60_000L) // 1 minute maximum
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire alarm wake lock: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Alarm Ringing",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority alarm when study timer completes"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500, 1000)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissAlarm()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AlarmRingingService"
        const val CHANNEL_ID = "timey_ringing_alarm_channel"
        const val NOTIFICATION_ID = 8805

        const val ACTION_START_RINGING = "com.timey.app.ACTION_START_RINGING"
        const val ACTION_DISMISS_ALARM = "com.timey.app.ACTION_DISMISS_ALARM"

        const val EXTRA_SESSION_TITLE = "extra_session_title"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"

        fun startRinging(context: Context, sessionTitle: String, durationMinutes: Int) {
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                action = ACTION_START_RINGING
                putExtra(EXTRA_SESSION_TITLE, sessionTitle)
                putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                action = ACTION_DISMISS_ALARM
            }
            context.startService(intent)
        }
    }
}
