package com.timey.app.core.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.timey.app.MainActivity;
import com.timey.app.R;

/**
 * Java BroadcastReceiver that receives exact alarm triggers from AlarmManager.
 * Demonstrates robust hardware wakeup, vibration pattern dispatch, and heads-up notifications.
 */
public class AlarmBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmBroadcastReceiver";
    public static final String ALARM_CHANNEL_ID = "timey_alarm_channel";
    public static final int NOTIFICATION_ID = 8802;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        String action = intent.getAction();
        Log.i(TAG, "Alarm broadcast received with action: " + action);

        if (ExactAlarmScheduler.ACTION_TIMER_EXPIRED.equals(action)) {
            String sessionTitle = intent.getStringExtra(ExactAlarmScheduler.EXTRA_SESSION_TITLE);
            if (sessionTitle == null || sessionTitle.trim().isEmpty()) {
                sessionTitle = "Focus Block";
            }
            int duration = intent.getIntExtra(ExactAlarmScheduler.EXTRA_DURATION_MINUTES, 25);

            triggerHapticFeedback(context);
            showCompletionNotification(context, sessionTitle, duration);
        }
    }

    private void triggerHapticFeedback(@NonNull Context context) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] timings = new long[]{0, 400, 200, 400, 200, 600};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, -1));
                } else {
                    vibrator.vibrate(timings, -1);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to trigger vibration: " + e.getMessage());
        }
    }

    private void showCompletionNotification(
            @NonNull Context context,
            @NonNull String sessionTitle,
            int durationMinutes
    ) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        createAlarmNotificationChannel(notificationManager);

        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }

        Notification notification = new NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("🎉 " + sessionTitle + " Completed!")
                .setContentText("Great work! You finished " + durationMinutes + " minutes of uninterrupted focus.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setSound(alarmSound)
                .setContentIntent(contentIntent)
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public static void createAlarmNotificationChannel(@NonNull NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel existing = manager.getNotificationChannel(ALARM_CHANNEL_ID);
            if (existing != null) return;

            NotificationChannel channel = new NotificationChannel(
                    ALARM_CHANNEL_ID,
                    "Study Completion Alarms",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("High-priority alarm chime when a Pomodoro or study session finishes");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 400, 200, 400, 200, 600});
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();
            channel.setSound(defaultSound, audioAttributes);

            manager.createNotificationChannel(channel);
        }
    }
}
