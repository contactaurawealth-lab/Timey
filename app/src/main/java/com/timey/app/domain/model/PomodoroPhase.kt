package com.timey.app.domain.model

enum class PomodoroPhase(
    val label: String,
    val defaultMinutes: Int,
    val isBreak: Boolean = false
) {
    FOCUS("Focus Block", 25, isBreak = false),
    SHORT_BREAK("Short Rest", 5, isBreak = true),
    LONG_BREAK("Deep Recovery", 15, isBreak = true)
}
