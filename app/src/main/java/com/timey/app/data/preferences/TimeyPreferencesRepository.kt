package com.timey.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.timey.app.domain.model.AlarmSound
import com.timey.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "timey_settings")

data class TimeySettings(
    val pomodoroStudyMinutes: Int = 25,
    val pomodoroShortBreakMinutes: Int = 5,
    val pomodoroLongBreakMinutes: Int = 15,
    val deepWorkStudyMinutes: Int = 135,
    val deepWorkBreakMinutes: Int = 15,
    val targetCycles: Int = 4,
    val autoStartBreaks: Boolean = false,
    val autoStartPomodoros: Boolean = false,
    val keepScreenAwake: Boolean = true,
    val strictModeEnabled: Boolean = false,
    val appBlockingEnabled: Boolean = false,
    val blockedPackages: Set<String> = emptySet(),
    val pinProtectionEnabled: Boolean = false,
    val securityPin: String = "1234",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColorIndex: Int = 0,
    val alarmSound: AlarmSound = AlarmSound.ZEN_BELL,
    val alarmVolume: Float = 0.85f,
    val musicVolume: Float = 0.70f,
    val selectedSoundscape: String = "RAIN",
    val lastSubject: String = "Mathematics",
    val lastTopic: String = "Calculus & Integrals"
)

class TimeyPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val POMODORO_STUDY_MIN = intPreferencesKey("pomodoro_study_min")
        val POMODORO_SHORT_BREAK_MIN = intPreferencesKey("pomodoro_short_break_min")
        val POMODORO_LONG_BREAK_MIN = intPreferencesKey("pomodoro_long_break_min")
        val DEEP_WORK_STUDY_MIN = intPreferencesKey("deep_work_study_min")
        val DEEP_WORK_BREAK_MIN = intPreferencesKey("deep_work_break_min")
        val TARGET_CYCLES = intPreferencesKey("target_cycles")
        val AUTO_START_BREAKS = booleanPreferencesKey("auto_start_breaks")
        val AUTO_START_POMODOROS = booleanPreferencesKey("auto_start_pomodoros")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val STRICT_MODE = booleanPreferencesKey("strict_mode")
        val APP_BLOCKING_ENABLED = booleanPreferencesKey("app_blocking_enabled")
        val BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")
        val PIN_PROTECTION_ENABLED = booleanPreferencesKey("pin_protection_enabled")
        val SECURITY_PIN = stringPreferencesKey("security_pin")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR_INDEX = intPreferencesKey("accent_color_index")
        val ALARM_SOUND = stringPreferencesKey("alarm_sound")
        val ALARM_VOLUME = floatPreferencesKey("alarm_volume")
        val MUSIC_VOLUME = floatPreferencesKey("music_volume")
        val SELECTED_SOUNDSCAPE = stringPreferencesKey("selected_soundscape")
        val LAST_SUBJECT = stringPreferencesKey("last_subject")
        val LAST_TOPIC = stringPreferencesKey("last_topic")
    }

    val settingsFlow: Flow<TimeySettings> = context.dataStore.data.map { prefs ->
        val themeString = prefs[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = try {
            ThemeMode.valueOf(themeString)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }

        val alarmString = prefs[PreferencesKeys.ALARM_SOUND] ?: AlarmSound.ZEN_BELL.name
        val alarmSound = try {
            AlarmSound.valueOf(alarmString)
        } catch (_: Exception) {
            AlarmSound.ZEN_BELL
        }

        TimeySettings(
            pomodoroStudyMinutes = prefs[PreferencesKeys.POMODORO_STUDY_MIN] ?: 25,
            pomodoroShortBreakMinutes = prefs[PreferencesKeys.POMODORO_SHORT_BREAK_MIN] ?: 5,
            pomodoroLongBreakMinutes = prefs[PreferencesKeys.POMODORO_LONG_BREAK_MIN] ?: 15,
            deepWorkStudyMinutes = prefs[PreferencesKeys.DEEP_WORK_STUDY_MIN] ?: 135,
            deepWorkBreakMinutes = prefs[PreferencesKeys.DEEP_WORK_BREAK_MIN] ?: 15,
            targetCycles = prefs[PreferencesKeys.TARGET_CYCLES] ?: 4,
            autoStartBreaks = prefs[PreferencesKeys.AUTO_START_BREAKS] ?: false,
            autoStartPomodoros = prefs[PreferencesKeys.AUTO_START_POMODOROS] ?: false,
            keepScreenAwake = prefs[PreferencesKeys.KEEP_SCREEN_AWAKE] ?: true,
            strictModeEnabled = prefs[PreferencesKeys.STRICT_MODE] ?: false,
            appBlockingEnabled = prefs[PreferencesKeys.APP_BLOCKING_ENABLED] ?: false,
            blockedPackages = prefs[PreferencesKeys.BLOCKED_PACKAGES] ?: setOf(
                "com.instagram.android",
                "com.zhiliaoapp.musically",
                "com.twitter.android",
                "com.google.android.youtube",
                "com.reddit.frontpage",
                "com.snapchat.android",
                "com.facebook.katana",
                "com.discord"
            ),
            pinProtectionEnabled = prefs[PreferencesKeys.PIN_PROTECTION_ENABLED] ?: false,
            securityPin = prefs[PreferencesKeys.SECURITY_PIN] ?: "1234",
            themeMode = themeMode,
            accentColorIndex = prefs[PreferencesKeys.ACCENT_COLOR_INDEX] ?: 0,
            alarmSound = alarmSound,
            alarmVolume = prefs[PreferencesKeys.ALARM_VOLUME] ?: 0.85f,
            musicVolume = prefs[PreferencesKeys.MUSIC_VOLUME] ?: 0.70f,
            selectedSoundscape = prefs[PreferencesKeys.SELECTED_SOUNDSCAPE] ?: "RAIN",
            lastSubject = prefs[PreferencesKeys.LAST_SUBJECT] ?: "Mathematics",
            lastTopic = prefs[PreferencesKeys.LAST_TOPIC] ?: "Calculus & Integrals"
        )
    }

    suspend fun updatePomodoroMinutes(study: Int, shortBreak: Int, longBreak: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.POMODORO_STUDY_MIN] = study
            prefs[PreferencesKeys.POMODORO_SHORT_BREAK_MIN] = shortBreak
            prefs[PreferencesKeys.POMODORO_LONG_BREAK_MIN] = longBreak
        }
    }

    suspend fun updateDeepWorkMinutes(study: Int, breakMinutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DEEP_WORK_STUDY_MIN] = study
            prefs[PreferencesKeys.DEEP_WORK_BREAK_MIN] = breakMinutes
        }
    }

    suspend fun updateTargetCycles(cycles: Int) {
        context.dataStore.edit { it[PreferencesKeys.TARGET_CYCLES] = cycles }
    }

    suspend fun updateAutoStart(breaks: Boolean, pomodoros: Boolean) {
        context.dataStore.edit {
            it[PreferencesKeys.AUTO_START_BREAKS] = breaks
            it[PreferencesKeys.AUTO_START_POMODOROS] = pomodoros
        }
    }

    suspend fun updateAppBlocking(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.APP_BLOCKING_ENABLED] = enabled }
    }

    suspend fun setBlockedPackages(packages: Set<String>) {
        context.dataStore.edit { it[PreferencesKeys.BLOCKED_PACKAGES] = packages }
    }

    suspend fun toggleBlockedPackage(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.BLOCKED_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            prefs[PreferencesKeys.BLOCKED_PACKAGES] = current
        }
    }

    suspend fun updatePinProtection(enabled: Boolean, pin: String) {
        context.dataStore.edit {
            it[PreferencesKeys.PIN_PROTECTION_ENABLED] = enabled
            if (pin.isNotBlank()) it[PreferencesKeys.SECURITY_PIN] = pin
        }
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = themeMode.name }
    }

    suspend fun updateAccentColorIndex(index: Int) {
        context.dataStore.edit { it[PreferencesKeys.ACCENT_COLOR_INDEX] = index }
    }

    suspend fun updateAlarmSound(sound: AlarmSound) {
        context.dataStore.edit { it[PreferencesKeys.ALARM_SOUND] = sound.name }
    }

    suspend fun updateAlarmVolume(volume: Float) {
        context.dataStore.edit { it[PreferencesKeys.ALARM_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun updateMusicVolume(volume: Float) {
        context.dataStore.edit { it[PreferencesKeys.MUSIC_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun updateSelectedSoundscape(soundscapeKey: String) {
        context.dataStore.edit { it[PreferencesKeys.SELECTED_SOUNDSCAPE] = soundscapeKey }
    }

    suspend fun updateSubjectAndTopic(subject: String, topic: String) {
        context.dataStore.edit {
            it[PreferencesKeys.LAST_SUBJECT] = subject
            it[PreferencesKeys.LAST_TOPIC] = topic
        }
    }
}
