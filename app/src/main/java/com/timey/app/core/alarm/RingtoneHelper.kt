package com.timey.app.core.alarm

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

data class SystemRingtone(
    val title: String,
    val uri: Uri
)

object RingtoneHelper {

    fun getAvailableSystemRingtones(context: Context): List<SystemRingtone> {
        val list = mutableListOf<SystemRingtone>()
        try {
            val ringtoneMgr = RingtoneManager(context)
            ringtoneMgr.setType(RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
            val cursor = ringtoneMgr.cursor
            while (cursor != null && cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = ringtoneMgr.getRingtoneUri(cursor.position)
                if (title != null && uri != null) {
                    list.add(SystemRingtone(title, uri))
                }
            }
        } catch (_: Exception) {}
        return list
    }
}
