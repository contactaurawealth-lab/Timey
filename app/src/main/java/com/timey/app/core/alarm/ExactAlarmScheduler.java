package com.timey.app.core.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.timey.app.MainActivity;

/**
 * ExactAlarmScheduler handles precise hardware alarm scheduling using Android's AlarmManager.
 * Demonstrates high-performance Java-Kotlin interop with strict nullability annotations.
 */
public final class ExactAlarmScheduler {

    private static final String TAG = "ExactAlarmScheduler";
    public static final String ACTION_TIMER_EXPIRED = "com.timey.app.ACTION_TIMER_EXPIRED";
    public static final String EXTRA_SESSION_TITLE = "extra_session_title";
    public static final String EXTRA_DURATION_MINUTES = "extra_duration_minutes";
    public static final int ALARM_REQUEST_CODE = 4401;

    private final Context context;
    private final AlarmManager alarmManager;

    public ExactAlarmScheduler(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * Checks if the app has permission to schedule exact alarms (required on Android 12+ API 31).
     */
    public boolean canScheduleExactAlarms() {
        if (alarmManager == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    /**
     * Schedules an exact alarm that wakes the device from deep sleep (Doze mode).
     * Uses AlarmClockInfo for the highest OS priority, bypassing OEM task killers.
     *
     * @param triggerAtMillis Wall-clock time (System.currentTimeMillis()) when alarm should fire.
     * @param sessionTitle    Title of the focus session to display in notification.
     * @param durationMinutes Total duration in minutes completed.
     */
    public void scheduleExactAlarm(
            long triggerAtMillis,
            @NonNull String sessionTitle,
            int durationMinutes
    ) {
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager service is null. Cannot schedule alarm.");
            return;
        }

        PendingIntent alarmIntent = createAlarmPendingIntent(sessionTitle, durationMinutes);

        try {
            // Intent to show when the user taps on the alarm clock in system UI
            Intent showIntent = new Intent(context, MainActivity.class);
            showIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent showPendingIntent = PendingIntent.getActivity(
                    context,
                    ALARM_REQUEST_CODE + 1,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // setAlarmClock is the gold standard for timers/alarms on Android.
            // It completely bypasses Doze mode restrictions and shows an alarm icon in the status bar.
            AlarmManager.AlarmClockInfo clockInfo = new AlarmManager.AlarmClockInfo(
                    triggerAtMillis,
                    showPendingIntent
            );
            alarmManager.setAlarmClock(clockInfo, alarmIntent);
            Log.d(TAG, "Exact AlarmClock scheduled successfully for timestamp: " + triggerAtMillis);

        } catch (SecurityException e) {
            Log.w(TAG, "SecurityException scheduling exact alarm. Falling back to setExactAndAllowWhileIdle: " + e.getMessage());
            // Fallback for restricted permission states
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    alarmIntent
            );
        }
    }

    /**
     * Cancels any previously scheduled timer alarm.
     */
    public void cancelAlarm() {
        if (alarmManager == null) return;
        PendingIntent pendingIntent = createAlarmPendingIntent("", 0);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
        Log.d(TAG, "Exact alarm cancelled.");
    }

    @NonNull
    private PendingIntent createAlarmPendingIntent(
            @NonNull String sessionTitle,
            int durationMinutes
    ) {
        Intent intent = new Intent(context, AlarmBroadcastReceiver.class);
        intent.setAction(ACTION_TIMER_EXPIRED);
        intent.putExtra(EXTRA_SESSION_TITLE, sessionTitle);
        intent.putExtra(EXTRA_DURATION_MINUTES, durationMinutes);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                flags
        );
    }
}
