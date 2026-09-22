package com.timey.app.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.timey.app.MainActivity
import com.timey.app.R
import com.timey.app.core.alarm.ExactAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Android 14+ (API 34) Compliant Foreground Service for continuous timer execution.
 * Maintains wake lock and delivers real-time notifications with interactive actions.
 */
class FocusForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    private var timerTickerJob: Job? = null
    private lateinit var exactAlarmScheduler: ExactAlarmScheduler

    override fun onCreate() {
        super.onCreate()
        exactAlarmScheduler = ExactAlarmScheduler(this)
        createNotificationChannel()
        acquireWakeLock()
        Log.d(TAG, "FocusForegroundService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_TIMER

        when (action) {
            ACTION_START_TIMER -> {
                val totalSeconds = intent?.getIntExtra(EXTRA_DURATION_SECONDS, 25 * 60) ?: (25 * 60)
                val sessionTitle = intent?.getStringExtra(EXTRA_SESSION_TITLE) ?: "Focus Block"
                startTimer(totalSeconds, sessionTitle)
            }
            ACTION_PAUSE_TIMER -> pauseTimer()
            ACTION_RESUME_TIMER -> resumeTimer()
            ACTION_ADD_FIVE_MINUTES -> addFiveMinutes()
            ACTION_STOP_TIMER -> stopTimer()
        }

        return START_NOT_STICKY
    }

    private fun startTimer(totalSeconds: Int, sessionTitle: String) {
        currentTitle = sessionTitle
        targetEndRealtime = SystemClock.elapsedRealtime() + (totalSeconds * 1000L)
        _serviceTimerState.value = ServiceTimerState(
            isRunning = true,
            isPaused = false,
            remainingSeconds = totalSeconds,
            totalSeconds = totalSeconds,
            sessionTitle = sessionTitle
        )

        // Schedule exact hardware alarm for when timer expires
        val wallClockTarget = System.currentTimeMillis() + (totalSeconds * 1000L)
        exactAlarmScheduler.scheduleExactAlarm(wallClockTarget, sessionTitle, totalSeconds / 60)

        // Start Foreground with Android 14 FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        val notification = buildTimerNotification(totalSeconds, isPaused = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startTicker()
    }

    private fun startTicker() {
        timerTickerJob?.cancel()
        timerTickerJob = serviceScope.launch {
            while (true) {
                delay(1000L)
                val remainingMillis = targetEndRealtime - SystemClock.elapsedRealtime()
                val remainingSec = ((remainingMillis + 999) / 1000).toInt().coerceAtLeast(0)

                _serviceTimerState.value = _serviceTimerState.value.copy(
                    remainingSeconds = remainingSec
                )

                if (remainingSec <= 0) {
                    onTimerFinished()
                    break
                } else {
                    updateNotification(remainingSec, isPaused = false)
                }
            }
        }
    }

    private fun pauseTimer() {
        timerTickerJob?.cancel()
        timerTickerJob = null
        exactAlarmScheduler.cancelAlarm()

        val remaining = _serviceTimerState.value.remainingSeconds
        _serviceTimerState.value = _serviceTimerState.value.copy(
            isRunning = false,
            isPaused = true
        )
        updateNotification(remaining, isPaused = true)
    }

    private fun resumeTimer() {
        val remaining = _serviceTimerState.value.remainingSeconds
        if (remaining <= 0) return

        targetEndRealtime = SystemClock.elapsedRealtime() + (remaining * 1000L)
        val wallClockTarget = System.currentTimeMillis() + (remaining * 1000L)
        exactAlarmScheduler.scheduleExactAlarm(wallClockTarget, currentTitle, remaining / 60)

        _serviceTimerState.value = _serviceTimerState.value.copy(
            isRunning = true,
            isPaused = false
        )
        startTicker()
    }

    private fun addFiveMinutes() {
        val additionalSec = 5 * 60
        targetEndRealtime += (additionalSec * 1000L)
        val newRemaining = _serviceTimerState.value.remainingSeconds + additionalSec
        val newTotal = _serviceTimerState.value.totalSeconds + additionalSec

        exactAlarmScheduler.scheduleExactAlarm(
            System.currentTimeMillis() + (newRemaining * 1000L),
            currentTitle,
            newRemaining / 60
        )

        _serviceTimerState.value = _serviceTimerState.value.copy(
            remainingSeconds = newRemaining,
            totalSeconds = newTotal
        )
        updateNotification(newRemaining, _serviceTimerState.value.isPaused)
    }

    private fun onTimerFinished() {
        _serviceTimerState.value = _serviceTimerState.value.copy(
            isRunning = false,
            isPaused = false,
            remainingSeconds = 0
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopTimer() {
        timerTickerJob?.cancel()
        exactAlarmScheduler.cancelAlarm()
        _serviceTimerState.value = ServiceTimerState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildTimerNotification(remainingSeconds: Int, isPaused: Boolean): Notification {
        val m = remainingSeconds / 60
        val s = remainingSeconds % 60
        val formattedTime = String.format("%02d:%02d", m, s)
        val statusText = if (isPaused) "Paused • $formattedTime left" else "Focusing • $formattedTime left"

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action Intents
        val pauseResumeAction = if (isPaused) {
            val resumeIntent = Intent(this, FocusForegroundService::class.java).apply {
                action = ACTION_RESUME_TIMER
            }
            val resumePendingIntent = PendingIntent.getService(
                this, 1, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action.Builder(R.drawable.ic_play, "Resume", resumePendingIntent).build()
        } else {
            val pauseIntent = Intent(this, FocusForegroundService::class.java).apply {
                action = ACTION_PAUSE_TIMER
            }
            val pausePendingIntent = PendingIntent.getService(
                this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action.Builder(R.drawable.ic_pause, "Pause", pausePendingIntent).build()
        }

        val addFiveIntent = Intent(this, FocusForegroundService::class.java).apply {
            action = ACTION_ADD_FIVE_MINUTES
        }
        val addFivePending = PendingIntent.getService(
            this, 3, addFiveIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FocusForegroundService::class.java).apply {
            action = ACTION_STOP_TIMER
        }
        val stopPending = PendingIntent.getService(
            this, 4, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(currentTitle)
            .setContentText(statusText)
            .setOngoing(!isPaused)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .addAction(pauseResumeAction)
            .addAction(R.drawable.ic_timer, "+5 Min", addFivePending)
            .addAction(R.drawable.ic_skip, "Finish", stopPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun updateNotification(remainingSeconds: Int, isPaused: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildTimerNotification(remainingSeconds, isPaused))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Study Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Displays the active countdown timer and session actions"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Timey:TimerWakeLock"
            )?.apply {
                // Acquire with timeout to avoid permanent drain
                acquire(4 * 60 * 60 * 1000L) // 4 hours maximum
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        Log.d(TAG, "FocusForegroundService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "FocusForegroundService"
        const val CHANNEL_ID = "timey_timer_service_channel"
        const val NOTIFICATION_ID = 7701

        const val ACTION_START_TIMER = "com.timey.app.ACTION_START_TIMER"
        const val ACTION_PAUSE_TIMER = "com.timey.app.ACTION_PAUSE_TIMER"
        const val ACTION_RESUME_TIMER = "com.timey.app.ACTION_RESUME_TIMER"
        const val ACTION_ADD_FIVE_MINUTES = "com.timey.app.ACTION_ADD_FIVE_MINUTES"
        const val ACTION_STOP_TIMER = "com.timey.app.ACTION_STOP_TIMER"

        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"
        const val EXTRA_SESSION_TITLE = "extra_session_title"

        private var targetEndRealtime: Long = 0L
        private var currentTitle: String = "Focus Block"

        private val _serviceTimerState = MutableStateFlow(ServiceTimerState())
        val serviceTimerState: StateFlow<ServiceTimerState> = _serviceTimerState.asStateFlow()

        fun start(context: Context, totalSeconds: Int, title: String) {
            val intent = Intent(context, FocusForegroundService::class.java).apply {
                action = ACTION_START_TIMER
                putExtra(EXTRA_DURATION_SECONDS, totalSeconds)
                putExtra(EXTRA_SESSION_TITLE, title)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pause(context: Context) {
            val intent = Intent(context, FocusForegroundService::class.java).apply {
                action = ACTION_PAUSE_TIMER
            }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, FocusForegroundService::class.java).apply {
                action = ACTION_RESUME_TIMER
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, FocusForegroundService::class.java).apply {
                action = ACTION_STOP_TIMER
            }
            context.startService(intent)
        }
    }
}

data class ServiceTimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val sessionTitle: String = "Focus Block"
)
