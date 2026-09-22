package com.timey.app

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import com.timey.app.core.alarm.AlarmBroadcastReceiver

class TimeyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        AlarmBroadcastReceiver.createAlarmNotificationChannel(notificationManager)
    }
}
