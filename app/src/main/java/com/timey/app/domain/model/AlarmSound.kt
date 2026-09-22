package com.timey.app.domain.model

enum class AlarmSound(
    val title: String,
    val description: String,
    val isSilent: Boolean = false
) {
    ZEN_BELL(
        title = "Zen Temple Bell",
        description = "Serene harmonic gong with warm acoustic decay."
    ),
    GENTLE_CHIME(
        title = "Gentle Arpeggio",
        description = "Soft 4-tone ascending chord progression."
    ),
    DIGITAL_PULSE(
        title = "Focus Pulse",
        description = "Crisp, non-jarring dual beep."
    ),
    MORNING_BIRDS(
        title = "Serene Morning",
        description = "Delicate ambient frequency sweep."
    ),
    SILENT_VIBRATE(
        title = "Silent Vibration Only",
        description = "Tactile haptics only, ideal for libraries and quiet study halls.",
        isSilent = true
    ),
    SYSTEM_DEFAULT(
        title = "System Alarm Tone",
        description = "Standard Android ringtone or alarm sound."
    ),
    CUSTOM_USER_AUDIO(
        title = "Custom Audio File",
        description = "Your personal imported ringtone or song."
    )
}
