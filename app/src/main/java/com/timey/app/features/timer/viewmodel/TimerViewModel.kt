package com.timey.app.features.timer.viewmodel

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timey.app.core.alarm.ExactAlarmScheduler
import com.timey.app.core.service.FocusForegroundService
import com.timey.app.domain.model.PomodoroPhase
import com.timey.app.domain.model.TimerMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val lastCompletedMinutes: Int = 0
)

class TimerViewModel(
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private val alarmScheduler = ExactAlarmScheduler(context)
    private var tickerJob: Job? = null
    private var distractionJob: Job? = null

    // Anchors for monotonic clock
    private var targetEndRealtime: Long = 0L
    private var startRealtime: Long = 0L

    init {
        // Observe foreground service state changes
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
    }

    fun setTimerMode(mode: TimerMode) {
        if (_uiState.value.status == TimerStatus.RUNNING) return
        resetTimer()

        val defaultSec = when (mode) {
            TimerMode.POMODORO -> 25 * 60
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

        if (state.mode == TimerMode.FLOWMODORO || state.mode == TimerMode.STOPWATCH) {
            startRealtime = now - (state.elapsedSeconds * 1000L)
        } else {
            targetEndRealtime = now + (state.remainingSeconds * 1000L)
            // Schedule exact hardware alarm
            val wallClockTarget = System.currentTimeMillis() + (state.remainingSeconds * 1000L)
            alarmScheduler.scheduleExactAlarm(
                wallClockTarget,
                "${state.sessionSubject}: ${state.sessionTopic}",
                state.remainingSeconds / 60
            )
        }

        _uiState.update { it.copy(status = TimerStatus.RUNNING) }

        // Start native Foreground Service
        FocusForegroundService.start(
            context,
            if (state.mode == TimerMode.FLOWMODORO || state.mode == TimerMode.STOPWATCH) state.elapsedSeconds else state.remainingSeconds,
            "${state.sessionSubject} (${state.mode.title})"
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
            // Strict Exam mode disallows pause
            return
        }
        tickerJob?.cancel()
        alarmScheduler.cancelAlarm()
        FocusForegroundService.pause(context)
        _uiState.update { it.copy(status = TimerStatus.PAUSED) }
    }

    fun resumeTimer() {
        startTimer()
    }

    fun resetTimer() {
        tickerJob?.cancel()
        alarmScheduler.cancelAlarm()
        FocusForegroundService.stop(context)

        val mode = _uiState.value.mode
        val resetSec = if (mode == TimerMode.FLOWMODORO || mode == TimerMode.STOPWATCH) 0 else _uiState.value.totalDurationSeconds

        _uiState.update {
            it.copy(
                status = TimerStatus.IDLE,
                remainingSeconds = resetSec,
                elapsedSeconds = 0,
                isDistractionWarningActive = false
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

        awardCompanionXp(completedMinutes)

        if (state.mode == TimerMode.POMODORO) {
            if (state.pomodoroPhase == PomodoroPhase.FOCUS) {
                val nextCycle = state.completedCycles + 1
                val isLongBreak = (nextCycle % state.targetCyclesBeforeLongBreak == 0)
                val nextPhase = if (isLongBreak) PomodoroPhase.LONG_BREAK else PomodoroPhase.SHORT_BREAK
                val nextDurationSec = nextPhase.defaultMinutes * 60

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
                // Break completed, switch back to Focus
                val nextPhase = PomodoroPhase.FOCUS
                val focusSec = 25 * 60
                _uiState.update {
                    it.copy(
                        status = TimerStatus.COMPLETED,
                        pomodoroPhase = nextPhase,
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

    // Strict Mode: trigger grace countdown if user leaves app
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
                // Grace expired -> Abandon session
                abandonSessionDueToDistraction()
            }
        }
    }

    fun onAppReturnedToForeground() {
        distractionJob?.cancel()
        _uiState.update {
            it.copy(isDistractionWarningActive = false, distractionGraceSecondsRemaining = 10)
        }
    }

    private fun abandonSessionDueToDistraction() {
        resetTimer()
        _uiState.update {
            it.copy(
                isDistractionWarningActive = false,
                status = TimerStatus.IDLE
            )
        }
    }

    fun dismissRetrospective() {
        _uiState.update { it.copy(showRetrospectiveDialog = false) }
    }

    fun setSubjectAndTopic(subject: String, topic: String) {
        _uiState.update { it.copy(sessionSubject = subject, sessionTopic = topic) }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        distractionJob?.cancel()
    }
}
