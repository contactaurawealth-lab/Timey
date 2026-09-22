package com.timey.app.domain.model

enum class PomodoroPhase(
    val label: String,
    val defaultMinutes: Int
) {
    FOCUS("Focus Block", 25),
    SHORT_BREAK("Short Rest", 5),
    LONG_BREAK("Deep Recovery", 15)
}
