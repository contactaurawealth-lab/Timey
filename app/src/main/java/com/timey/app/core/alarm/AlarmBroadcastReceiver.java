package com.timey.app.core.alarm;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * BroadcastReceiver that receives hardware RTC alarm triggers from AlarmManager.
 * Launches the high-priority AlarmRingingService to ensure continuous ringing and vibration.
 */
public class AlarmBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        String action = intent.getAction();
        Log.i(TAG, "Alarm broadcast received with action: " + action);

        if (ExactAlarmScheduler.ACTION_TIMER_EXPIRED.equals(action) ||
            "com.timey.app.ACTION_TIMER_COMPLETED".equals(action)) {

            String sessionTitle = intent.getStringExtra(ExactAlarmScheduler.EXTRA_SESSION_TITLE);
            if (sessionTitle == null || sessionTitle.trim().isEmpty()) {
                sessionTitle = "Focus Block";
            }
            int duration = intent.getIntExtra(ExactAlarmScheduler.EXTRA_DURATION_MINUTES, 25);

            // Start foreground ringing service ensuring continuous alarm audio, vibration, and wake lock
            AlarmRingingService.Companion.startRinging(context, sessionTitle, duration);
        }
    }

    public static void createAlarmNotificationChannel(@NonNull NotificationManager manager) {
        // Channel creation delegated to AlarmRingingService and FocusForegroundService
    }
}
