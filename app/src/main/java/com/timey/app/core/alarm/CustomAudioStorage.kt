package com.timey.app.core.alarm

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object CustomAudioStorage {

    private const val TAG = "CustomAudioStorage"
    private const val PREFS_NAME = "timey_audio_prefs"
    private const val KEY_CUSTOM_TITLE = "custom_audio_title"
    private const val CUSTOM_FILE_NAME = "custom_user_alarm_sound.bin"

    fun importAudioUri(context: Context, uri: Uri): Pair<File, String>? {
        return try {
            val contentResolver = context.contentResolver
            val displayName = queryDisplayName(context, uri) ?: "My Custom Audio"

            val targetFile = File(context.filesDir, CUSTOM_FILE_NAME)
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            saveCustomAudioTitle(context, displayName)
            Log.d(TAG, "Custom audio saved to ${targetFile.absolutePath} with title: $displayName")
            Pair(targetFile, displayName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import custom audio: ${e.message}", e)
            null
        }
    }

    fun getStoredCustomAudioFile(context: Context): File? {
        val file = File(context.filesDir, CUSTOM_FILE_NAME)
        return if (file.exists() && file.length() > 0) file else null
    }

    fun getStoredCustomAudioTitle(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_TITLE, null)
    }

    private fun saveCustomAudioTitle(context: Context, title: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_TITLE, title).apply()
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        var name: String? = null
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not query display name: ${e.message}")
        }
        return name ?: uri.lastPathSegment
    }
}
