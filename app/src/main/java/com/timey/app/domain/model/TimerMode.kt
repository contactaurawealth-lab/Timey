package com.timey.app.domain.model

enum class TimerMode(
    val title: String,
    val description: String,
    val defaultMinutes: Int
) {
    POMODORO(
        title = "Pomodoro",
        description = "Classic 25/5 rhythm for disciplined focus and steady cognitive recovery.",
        defaultMinutes = 25
    ),
    ULTRADIAN(
        title = "Ultradian (50/10)",
        description = "Deep 50-minute cognitive sprints matching natural human alertness cycles.",
        defaultMinutes = 50
    ),
    FLOWMODORO(
        title = "Flowmodoro",
        description = "Unconstrained count-up flow. When you stop, your break is calculated automatically.",
        defaultMinutes = 0
    ),
    EXAM(
        title = "Exam Simulator",
        description = "Strict continuous countdown with halfway and 5-minute alerts. No pauses allowed.",
        defaultMinutes = 60
    ),
    STOPWATCH(
        title = "Open Focus",
        description = "Free stopwatch timer for continuous open-ended study or reading.",
        defaultMinutes = 0
    )
}
