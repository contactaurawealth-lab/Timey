package com.timey.app.features.timer.viewmodel

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timey.app.core.alarm.AlarmSoundSynthesizer
import com.timey.app.core.alarm.CustomAudioStorage
import com.timey.app.core.alarm.ExactAlarmScheduler
import com.timey.app.core.audio.MusicPlaylistManager
import com.timey.app.core.blocker.BlockerManager
import com.timey.app.core.service.FocusForegroundService
import com.timey.app.data.local.TimeyDatabase
import com.timey.app.data.local.entity.StudySessionEntity
import com.timey.app.data.local.repository.StudySessionRepository
import com.timey.app.data.preferences.TimeyPreferencesRepository
import com.timey.app.domain.model.AlarmSound
import com.timey.app.domain.model.PomodoroPhase
import com.timey.app.domain.model.ThemeMode
import com.timey.app.domain.model.TimerMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

data class CompanionState(
    val name: String = "Komodo",
    val title: String = "Focus Hatchling",
    val level: Int = 1,
    val totalXp: Int = 140,
    val xpForNextLevel: Int = 300,
    val streakDays: Int = 3,
    val roadProgressPercent: Float = 0.46f
)

data class TimerUiState(
    val mode: TimerMode = TimerMode.POMODORO,
    val status: TimerStatus = TimerStatus.IDLE,
    val pomodoroPhase: PomodoroPhase = PomodoroPhase.FOCUS,
    val totalDurationSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val elapsedSeconds: Int = 0,
    val completedCycles: Int = 0,
    val targetCyclesBeforeLongBreak: Int = 4,
    val pomodoroStudyMinutes: Int = 25,
    val pomodoroShortBreakMinutes: Int = 5,
    val pomodoroLongBreakMinutes: Int = 15,
    val deepWorkStudyMinutes: Int = 135,
    val deepWorkBreakMinutes: Int = 15,
    val flowmodoroBreakRatio: Float = 0.2f, // 1:5 ratio (20% break)
    val autoStartBreaks: Boolean = false,
    val autoStartPomodoros: Boolean = false,
    val keepScreenAwake: Boolean = true,
    val strictModeEnabled: Boolean = false,
    val isDistractionWarningActive: Boolean = false,
    val distractionGraceSecondsRemaining: Int = 10,
    val sessionSubject: String = "Mathematics",
    val sessionTopic: String = "Calculus & Integrals",
    val companion: CompanionState = CompanionState(),
    val showRetrospectiveDialog: Boolean = false,
    val showTerminationConfirmDialog: Boolean = false,
    val showTerminationPinDialog: Boolean = false,
    val isFullscreenFocus: Boolean = false,
    val lastCompletedMinutes: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColorIndex: Int = 0,
    val alarmSound: AlarmSound = AlarmSound.ZEN_BELL,
    val alarmVolume: Float = 0.85f,
    val musicVolume: Float = 0.70f,
    val isPreviewingAlarm: Boolean = false,
    val showCustomAlarmDialog: Boolean = false,
    val showCustomDurationDialog: Boolean = false,
    val customAudioTitle: String? = null,
    val hasCustomAudio: Boolean = false,
    val appBlockingEnabled: Boolean = false,
    val blockedPackages: Set<String> = emptySet(),
    val pinProtectionEnabled: Boolean = false,
    val securityPin: String = "1234"
)

class TimerViewModel(
    private val context: Context,
    val studySessionRepository: StudySessionRepository = StudySessionRepository(
        TimeyDatabase.getInstance(context).studySessionDao()
    ),
    val preferencesRepository: TimeyPreferencesRepository = TimeyPreferencesRepository(context),
    val musicPlaylistManager: MusicPlaylistManager = MusicPlaylistManager(context)
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private val alarmScheduler = ExactAlarmScheduler(context)
    private var tickerJob: Job? = null
    private var distractionJob: Job? = null

    // Monotonic clock anchors
    private var targetEndRealtime: Long = 0L
    private var startRealtime: Long = 0L
    private var sessionStartTimeMillis: Long = 0L

    init {
        // Collect persistent settings from DataStore
        viewModelScope.launch {
            preferencesRepository.settingsFlow.collect { settings ->
                _uiState.update { current ->
                    current.copy(
                        pomodoroStudyMinutes = settings.pomodoroStudyMinutes,
                        pomodoroShortBreakMinutes = settings.pomodoroShortBreakMinutes,
                        pomodoroLongBreakMinutes = settings.pomodoroLongBreakMinutes,
                        deepWorkStudyMinutes = settings.deepWorkStudyMinutes,
                        deepWorkBreakMinutes = settings.deepWorkBreakMinutes,
                        targetCyclesBeforeLongBreak = settings.targetCycles,
                        autoStartBreaks = settings.autoStartBreaks,
                        autoStartPomodoros = settings.autoStartPomodoros,
                        keepScreenAwake = settings.keepScreenAwake,
                        strictModeEnabled = settings.strictModeEnabled,
                        appBlockingEnabled = settings.appBlockingEnabled,
                        blockedPackages = settings.blockedPackages,
                        pinProtectionEnabled = settings.pinProtectionEnabled,
                        securityPin = settings.securityPin,
                        themeMode = settings.themeMode,
                        accentColorIndex = settings.accentColorIndex,
                        alarmSound = settings.alarmSound,
                        alarmVolume = settings.alarmVolume,
                        musicVolume = settings.musicVolume,
                        sessionSubject = settings.lastSubject,
                        sessionTopic = settings.lastTopic
                    )
                }

                // Synchronize BlockerManager
                BlockerManager.updateConfig(
                    blockingEnabled = settings.appBlockingEnabled,
                    packages = settings.blockedPackages,
                    pinEnabled = settings.pinProtectionEnabled,
                    pin = settings.securityPin
                )

                musicPlaylistManager.setVolume(settings.musicVolume)
            }
        }

        // Synchronize with Foreground Service
        viewModelScope.launch {
            FocusForegroundService.serviceTimerState.collect { svcState ->
                if (svcState.isRunning && _uiState.value.status != TimerStatus.RUNNING) {
                    _uiState.update {
                        it.copy(
                            status = TimerStatus.RUNNING,
                            remainingSeconds = svcState.remainingSeconds
                        )
                    }
                }
            }
        }

        // Load custom audio if previously imported
        val storedTitle = CustomAudioStorage.getStoredCustomAudioTitle(context)
        val storedFile = CustomAudioStorage.getStoredCustomAudioFile(context)
        if (storedTitle != null && storedFile != null) {
            _uiState.update {
                it.copy(
                    customAudioTitle = storedTitle,
                    hasCustomAudio = true
                )
            }
        }
    }

    fun setTimerMode(mode: TimerMode) {
        if (_uiState.value.status == TimerStatus.RUNNING) return
        resetTimer(saveSession = false)

        val defaultSec = when (mode) {
            TimerMode.POMODORO -> _uiState.value.pomodoroStudyMinutes * 60
            TimerMode.DEEP_WORK_135 -> _uiState.value.deepWorkStudyMinutes * 60
            TimerMode.ULTRADIAN -> 50 * 60
            TimerMode.EXAM -> 60 * 60
            TimerMode.FLOWMODORO, TimerMode.STOPWATCH -> 0
        }

        _uiState.update {
            it.copy(
                mode = mode,
                pomodoroPhase = PomodoroPhase.FOCUS,
                totalDurationSeconds = defaultSec,
                remainingSeconds = defaultSec,
                elapsedSeconds = 0,
                status = TimerStatus.IDLE
            )
        }
    }

    fun setDurationMinutes(minutes: Int) {
        if (_uiState.value.status == TimerStatus.RUNNING) return
        val totalSec = minutes * 60
        _uiState.update {
            it.copy(
                totalDurationSeconds = totalSec,
                remainingSeconds = totalSec,
                elapsedSeconds = 0,
                status = TimerStatus.IDLE
            )
        }
    }

    fun startTimer() {
        if (_uiState.value.status == TimerStatus.RUNNING) return

        val state = _uiState.value
        val now = SystemClock.elapsedRealtime()
        sessionStartTimeMillis = System.currentTimeMillis()

        if (state.mode == TimerMode.FLOWMODORO || state.mode == TimerMode.STOPWATCH) {
            startRealtime = now - (state.elapsedSeconds * 1000L)
        } else {
            targetEndRealtime = now + (state.remainingSeconds * 1000L)
            val wallClockTarget = System.currentTimeMillis() + (state.remainingSeconds * 1000L)
            alarmScheduler.scheduleExactAlarm(
                wallClockTarget,
                "${state.sessionSubject}: ${state.sessionTopic}",
                state.remainingSeconds / 60
            )
        }

        _uiState.update { it.copy(status = TimerStatus.RUNNING) }

        // Start Foreground Service
        FocusForegroundService.start(
            context,
            if (state.mode == TimerMode.FLOWMODORO || state.mode == TimerMode.STOPWATCH) state.elapsedSeconds else state.remainingSeconds,
            "${state.sessionSubject} (${state.mode.title})"
        )

        BlockerManager.updateSessionState(
            isActive = (state.pomodoroPhase == PomodoroPhase.FOCUS),
            subject = state.sessionSubject,
            remainingSec = state.remainingSeconds
        )

        startLocalTicker()
    }

    private fun startLocalTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(500L)
                val state = _uiState.value
                if (state.status != TimerStatus.RUNNING) break

                val now = SystemClock.elapsedRealtime()

                if (state.mode == TimerMode.FLOWMODORO || state.mode == TimerMode.STOPWATCH) {
                    val elapsedMillis = now - startRealtime
                    val newElapsed = (elapsedMillis / 1000L).toInt().coerceAtLeast(0)
                    if (newElapsed != state.elapsedSeconds) {
                        _uiState.update { it.copy(elapsedSeconds = newElapsed, remainingSeconds = newElapsed) }
                    }
                } else {
                    val remainingMillis = targetEndRealtime - now
                    val newRemaining = ((remainingMillis + 999L) / 1000L).toInt().coerceAtLeast(0)
                    val newElapsed = (state.totalDurationSeconds - newRemaining).coerceAtLeast(0)

                    if (newRemaining <= 0) {
                        handleSessionComplete()
                        break
                    } else if (newRemaining != state.remainingSeconds) {
                        _uiState.update {
                            it.copy(remainingSeconds = newRemaining, elapsedSeconds = newElapsed)
                        }
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        if (_uiState.value.mode == TimerMode.EXAM) {
            // Exam simulator forbids pause
            return
        }
        tickerJob?.cancel()
        alarmScheduler.cancelAlarm()
        FocusForegroundService.pause(context)
        _uiState.update { it.copy(status = TimerStatus.PAUSED) }
        BlockerManager.updateSessionState(false, _uiState.value.sessionSubject, _uiState.value.remainingSeconds)
    }

    fun resumeTimer() {
        startTimer()
    }

    /**
     * Skip to the next phase (e.g. Focus -> Break, or Break -> Focus).
     */
    fun skipCurrentPhase() {
        val state = _uiState.value

        // If currently in focus with at least 60 seconds studied, save partial session
        if (state.pomodoroPhase == PomodoroPhase.FOCUS && state.elapsedSeconds >= 60) {
            saveSessionToDatabase(
                isCompleted = false,
                durationSeconds = state.elapsedSeconds
            )
        }

        tickerJob?.cancel()
        alarmScheduler.cancelAlarm()
        FocusForegroundService.skip(context)

        if (state.mode == TimerMode.POMODORO || state.mode == TimerMode.DEEP_WORK_135) {
            if (state.pomodoroPhase == PomodoroPhase.FOCUS) {
                val nextCycle = state.completedCycles + 1
                val isLongBreak = (nextCycle % state.targetCyclesBeforeLongBreak == 0)

                val breakMin = if (state.mode == TimerMode.DEEP_WORK_135) {
                    state.deepWorkBreakMinutes
                } else {
                    if (isLongBreak) state.pomodoroLongBreakMinutes else state.pomodoroShortBreakMinutes
                }
                val breakSec = breakMin * 60
                val nextPhase = if (isLongBreak) PomodoroPhase.LONG_BREAK else PomodoroPhase.SHORT_BREAK

                _uiState.update {
                    it.copy(
                        status = TimerStatus.IDLE,
                        completedCycles = nextCycle,
                        pomodoroPhase = nextPhase,
                        totalDurationSeconds = breakSec,
                        remainingSeconds = breakSec,
                        elapsedSeconds = 0
                    )
                }
                if (state.autoStartBreaks) startTimer()
            } else {
                // Break -> Next Focus
                val focusMin = if (state.mode == TimerMode.DEEP_WORK_135) state.deepWorkStudyMinutes else state.pomodoroStudyMinutes
                val focusSec = focusMin * 60
                _uiState.update {
                    it.copy(
                        status = TimerStatus.IDLE,
                        pomodoroPhase = PomodoroPhase.FOCUS,
                        totalDurationSeconds = focusSec,
                        remainingSeconds = focusSec,
                        elapsedSeconds = 0
                    )
                }
                if (state.autoStartPomodoros) startTimer()
            }
        } else {
            resetTimer(saveSession = false)
        }
    }

    /**
     * Prevents accidental termination: requests confirmation or PIN if active.
     */
    fun requestResetTimer() {
        val state = _uiState.value
        if (state.status == TimerStatus.RUNNING) {
            if (state.pinProtectionEnabled) {
                _uiState.update { it.copy(showTerminationPinDialog = true) }
            } else {
                _uiState.update { it.copy(showTerminationConfirmDialog = true) }
            }
        } else {
            resetTimer(saveSession = false)
        }
    }

    fun dismissTerminationConfirm() {
        _uiState.update { it.copy(showTerminationConfirmDialog = false, showTerminationPinDialog = false) }
    }

    fun confirmTermination() {
        dismissTerminationConfirm()
        resetTimer(saveSession = true)
    }

    fun resetTimer(saveSession: Boolean = true) {
        val state = _uiState.value
        if (saveSession && state.elapsedSeconds >= 60 && state.pomodoroPhase == PomodoroPhase.FOCUS) {
            saveSessionToDatabase(isCompleted = false, durationSeconds = state.elapsedSeconds)
        }

        tickerJob?.cancel()
        alarmScheduler.cancelAlarm()
        FocusForegroundService.stop(context)
        BlockerManager.updateSessionState(false, "", 0)

        val resetSec = when (state.mode) {
            TimerMode.POMODORO -> state.pomodoroStudyMinutes * 60
            TimerMode.DEEP_WORK_135 -> state.deepWorkStudyMinutes * 60
            TimerMode.ULTRADIAN -> 50 * 60
            TimerMode.EXAM -> 60 * 60
            TimerMode.FLOWMODORO, TimerMode.STOPWATCH -> 0
        }

        _uiState.update {
            it.copy(
                status = TimerStatus.IDLE,
                remainingSeconds = resetSec,
                elapsedSeconds = 0,
                isDistractionWarningActive = false,
                pomodoroPhase = PomodoroPhase.FOCUS
            )
        }
    }

    fun finishFlowmodoroAndCalculateBreak() {
        tickerJob?.cancel()
        alarmScheduler.cancelAlarm()
        FocusForegroundService.stop(context)

        val state = _uiState.value
        val studiedMinutes = (state.elapsedSeconds / 60).coerceAtLeast(1)
        val breakMinutes = ((studiedMinutes * state.flowmodoroBreakRatio).toInt()).coerceAtLeast(1)
        val breakSeconds = breakMinutes * 60

        saveSessionToDatabase(isCompleted = true, durationSeconds = state.elapsedSeconds)
        awardCompanionXp(studiedMinutes)

        _uiState.update {
            it.copy(
                status = TimerStatus.IDLE,
                pomodoroPhase = PomodoroPhase.SHORT_BREAK,
                totalDurationSeconds = breakSeconds,
                remainingSeconds = breakSeconds,
                elapsedSeconds = 0,
                lastCompletedMinutes = studiedMinutes,
                showRetrospectiveDialog = true
            )
        }
    }

    private fun handleSessionComplete() {
        tickerJob?.cancel()
        FocusForegroundService.stop(context)

        val state = _uiState.value
        val completedMinutes = (state.totalDurationSeconds / 60).coerceAtLeast(1)

        // Save session to Room database
        if (state.pomodoroPhase == PomodoroPhase.FOCUS) {
            saveSessionToDatabase(isCompleted = true, durationSeconds = state.totalDurationSeconds)
            awardCompanionXp(completedMinutes)
        }

        AlarmSoundSynthesizer.playAlarmSound(context, state.alarmSound, state.alarmVolume)

        if (state.mode == TimerMode.POMODORO || state.mode == TimerMode.DEEP_WORK_135) {
            if (state.pomodoroPhase == PomodoroPhase.FOCUS) {
                val nextCycle = state.completedCycles + 1
                val isLongBreak = (nextCycle % state.targetCyclesBeforeLongBreak == 0)

                val breakMin = if (state.mode == TimerMode.DEEP_WORK_135) {
                    state.deepWorkBreakMinutes
                } else {
                    if (isLongBreak) state.pomodoroLongBreakMinutes else state.pomodoroShortBreakMinutes
                }
                val nextDurationSec = breakMin * 60
                val nextPhase = if (isLongBreak) PomodoroPhase.LONG_BREAK else PomodoroPhase.SHORT_BREAK

                _uiState.update {
                    it.copy(
                        status = TimerStatus.COMPLETED,
                        completedCycles = nextCycle,
                        pomodoroPhase = nextPhase,
                        totalDurationSeconds = nextDurationSec,
                        remainingSeconds = nextDurationSec,
                        elapsedSeconds = 0,
                        lastCompletedMinutes = completedMinutes,
                        showRetrospectiveDialog = true
                    )
                }

                if (state.autoStartBreaks) {
                    startTimer()
                }
            } else {
                // Break finished -> Next Focus Block
                val focusMin = if (state.mode == TimerMode.DEEP_WORK_135) state.deepWorkStudyMinutes else state.pomodoroStudyMinutes
                val focusSec = focusMin * 60
                _uiState.update {
                    it.copy(
                        status = TimerStatus.COMPLETED,
                        pomodoroPhase = PomodoroPhase.FOCUS,
                        totalDurationSeconds = focusSec,
                        remainingSeconds = focusSec,
                        elapsedSeconds = 0
                    )
                }
                if (state.autoStartPomodoros) {
                    startTimer()
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    status = TimerStatus.COMPLETED,
                    remainingSeconds = 0,
                    elapsedSeconds = state.totalDurationSeconds,
                    lastCompletedMinutes = completedMinutes,
                    showRetrospectiveDialog = true
                )
            }
        }
    }

    private fun saveSessionToDatabase(isCompleted: Boolean, durationSeconds: Int) {
        val state = _uiState.value
        val entity = StudySessionEntity(
            startTimeMillis = if (sessionStartTimeMillis > 0) sessionStartTimeMillis else System.currentTimeMillis() - (durationSeconds * 1000L),
            endTimeMillis = System.currentTimeMillis(),
            durationSeconds = durationSeconds,
            subject = state.sessionSubject,
            topic = state.sessionTopic,
            mode = state.mode.title,
            isCompleted = isCompleted,
            xpEarned = (durationSeconds / 60) * 10
        )
        viewModelScope.launch {
            studySessionRepository.saveSession(entity)
        }
    }

    private fun awardCompanionXp(minutes: Int) {
        val earnedXp = minutes * 10
        val currentComp = _uiState.value.companion
        val newTotalXp = currentComp.totalXp + earnedXp

        val newLevel = (newTotalXp / 300) + 1
        val levelTitles = listOf(
            "Focus Hatchling",
            "Road Explorer",
            "Dragon Guardian",
            "Scholar Dragon",
            "Zenith Mythic"
        )
        val title = levelTitles.getOrElse(newLevel - 1) { "Zenith Mythic" }
        val xpProgress = (newTotalXp % 300).toFloat() / 300f

        _uiState.update {
            it.copy(
                companion = currentComp.copy(
                    level = newLevel,
                    title = title,
                    totalXp = newTotalXp,
                    roadProgressPercent = xpProgress
                )
            )
        }
    }

    // Strict Mode Lifecycle Handlers
    fun onAppLeftForeground() {
        if (_uiState.value.status == TimerStatus.RUNNING && _uiState.value.strictModeEnabled) {
            _uiState.update {
                it.copy(
                    isDistractionWarningActive = true,
                    distractionGraceSecondsRemaining = 10
                )
            }
            distractionJob?.cancel()
            distractionJob = viewModelScope.launch {
                for (sec in 9 downTo 0) {
                    delay(1000L)
                    _uiState.update { it.copy(distractionGraceSecondsRemaining = sec) }
                }
                resetTimer(saveSession = true)
            }
        }
    }

    fun onAppReturnedToForeground() {
        distractionJob?.cancel()
        _uiState.update {
            it.copy(isDistractionWarningActive = false, distractionGraceSecondsRemaining = 10)
        }
    }

    fun setFullscreenFocus(enabled: Boolean) {
        _uiState.update { it.copy(isFullscreenFocus = enabled) }
    }

    fun verifySecurityPin(pin: String): Boolean {
        return pin == _uiState.value.securityPin
    }

    fun dismissRetrospective() {
        _uiState.update { it.copy(showRetrospectiveDialog = false) }
    }

    fun setSubjectAndTopic(subject: String, topic: String) {
        _uiState.update { it.copy(sessionSubject = subject, sessionTopic = topic) }
        viewModelScope.launch {
            preferencesRepository.updateSubjectAndTopic(subject, topic)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        viewModelScope.launch { preferencesRepository.updateThemeMode(mode) }
    }

    fun cycleThemeMode() {
        val next = when (_uiState.value.themeMode) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        setThemeMode(next)
    }

    fun setAccentColorIndex(index: Int) {
        _uiState.update { it.copy(accentColorIndex = index) }
        viewModelScope.launch { preferencesRepository.updateAccentColorIndex(index) }
    }

    fun setAlarmSound(sound: AlarmSound) {
        _uiState.update { it.copy(alarmSound = sound) }
        viewModelScope.launch { preferencesRepository.updateAlarmSound(sound) }
        previewAlarmSound(sound)
    }

    fun previewAlarmSound(sound: AlarmSound) {
        _uiState.update { it.copy(isPreviewingAlarm = true) }
        AlarmSoundSynthesizer.playAlarmSound(context, sound, _uiState.value.alarmVolume)
    }

    fun previewCurrentAlarmSound() {
        previewAlarmSound(_uiState.value.alarmSound)
    }

    fun stopAlarmPreview() {
        AlarmSoundSynthesizer.stop()
        _uiState.update { it.copy(isPreviewingAlarm = false) }
    }

    fun setAlarmVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _uiState.update { it.copy(alarmVolume = clamped) }
        viewModelScope.launch { preferencesRepository.updateAlarmVolume(clamped) }
    }

    fun setMusicVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _uiState.update { it.copy(musicVolume = clamped) }
        musicPlaylistManager.setVolume(clamped)
        viewModelScope.launch { preferencesRepository.updateMusicVolume(clamped) }
    }

    fun toggleCustomAlarmDialog(show: Boolean) {
        if (!show) stopAlarmPreview()
        _uiState.update { it.copy(showCustomAlarmDialog = show) }
    }

    fun toggleCustomDurationDialog(show: Boolean) {
        _uiState.update { it.copy(showCustomDurationDialog = show) }
    }

    fun setCustomDurationMinutes(minutes: Int) {
        setDurationMinutes(minutes.coerceIn(1, 360))
        toggleCustomDurationDialog(false)
    }

    fun setPomodoroStudyMinutes(minutes: Int) {
        val valid = minutes.coerceIn(1, 180)
        _uiState.update { it.copy(pomodoroStudyMinutes = valid) }
        if (_uiState.value.mode == TimerMode.POMODORO && _uiState.value.status == TimerStatus.IDLE) {
            setDurationMinutes(valid)
        }
        viewModelScope.launch {
            preferencesRepository.updatePomodoroMinutes(
                valid,
                _uiState.value.pomodoroShortBreakMinutes,
                _uiState.value.pomodoroLongBreakMinutes
            )
        }
    }

    fun setPomodoroShortBreakMinutes(minutes: Int) {
        val valid = minutes.coerceIn(1, 60)
        _uiState.update { it.copy(pomodoroShortBreakMinutes = valid) }
        viewModelScope.launch {
            preferencesRepository.updatePomodoroMinutes(
                _uiState.value.pomodoroStudyMinutes,
                valid,
                _uiState.value.pomodoroLongBreakMinutes
            )
        }
    }

    fun setPomodoroLongBreakMinutes(minutes: Int) {
        val valid = minutes.coerceIn(1, 90)
        _uiState.update { it.copy(pomodoroLongBreakMinutes = valid) }
        viewModelScope.launch {
            preferencesRepository.updatePomodoroMinutes(
                _uiState.value.pomodoroStudyMinutes,
                _uiState.value.pomodoroShortBreakMinutes,
                valid
            )
        }
    }

    fun setDeepWorkStudyMinutes(minutes: Int) {
        val valid = minutes.coerceIn(30, 240)
        _uiState.update { it.copy(deepWorkStudyMinutes = valid) }
        if (_uiState.value.mode == TimerMode.DEEP_WORK_135 && _uiState.value.status == TimerStatus.IDLE) {
            setDurationMinutes(valid)
        }
        viewModelScope.launch {
            preferencesRepository.updateDeepWorkMinutes(valid, _uiState.value.deepWorkBreakMinutes)
        }
    }

    fun setTargetCyclesBeforeLongBreak(cycles: Int) {
        val valid = cycles.coerceIn(2, 12)
        _uiState.update { it.copy(targetCyclesBeforeLongBreak = valid) }
        viewModelScope.launch { preferencesRepository.updateTargetCycles(valid) }
    }

    fun setAutoStartBreaks(enabled: Boolean) {
        _uiState.update { it.copy(autoStartBreaks = enabled) }
        viewModelScope.launch { preferencesRepository.updateAutoStart(enabled, _uiState.value.autoStartPomodoros) }
    }

    fun setAutoStartPomodoros(enabled: Boolean) {
        _uiState.update { it.copy(autoStartPomodoros = enabled) }
        viewModelScope.launch { preferencesRepository.updateAutoStart(_uiState.value.autoStartBreaks, enabled) }
    }

    fun setKeepScreenAwake(enabled: Boolean) {
        _uiState.update { it.copy(keepScreenAwake = enabled) }
    }

    fun setStrictModeEnabled(enabled: Boolean) {
        _uiState.update { it.copy(strictModeEnabled = enabled) }
    }

    fun toggleAppBlocking(enabled: Boolean) {
        _uiState.update { it.copy(appBlockingEnabled = enabled) }
        viewModelScope.launch { preferencesRepository.updateAppBlocking(enabled) }
    }

    fun setBlockedPackages(packages: Set<String>) {
        _uiState.update { it.copy(blockedPackages = packages) }
        viewModelScope.launch { preferencesRepository.setBlockedPackages(packages) }
    }

    fun toggleBlockedPackage(packageName: String) {
        viewModelScope.launch { preferencesRepository.toggleBlockedPackage(packageName) }
    }

    fun updatePinProtection(enabled: Boolean, pin: String) {
        _uiState.update { it.copy(pinProtectionEnabled = enabled, securityPin = pin.ifBlank { "1234" }) }
        viewModelScope.launch { preferencesRepository.updatePinProtection(enabled, pin) }
    }

    fun importCustomAudio(uri: Uri) {
        val result = CustomAudioStorage.importAudioUri(context, uri)
        if (result != null) {
            val (_, title) = result
            _uiState.update {
                it.copy(
                    alarmSound = AlarmSound.CUSTOM_USER_AUDIO,
                    customAudioTitle = title,
                    hasCustomAudio = true
                )
            }
            viewModelScope.launch {
                preferencesRepository.updateAlarmSound(AlarmSound.CUSTOM_USER_AUDIO)
            }
            previewAlarmSound(AlarmSound.CUSTOM_USER_AUDIO)
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        distractionJob?.cancel()
    }
}
