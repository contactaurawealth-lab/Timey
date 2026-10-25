package com.timey.app.core.audio

enum class SoundscapeType(
    val title: String,
    val subtitle: String,
    val iconEmoji: String
) {
    RAIN(
        title = "Gentle Rain",
        subtitle = "Soothing raindrops with distant thunder",
        iconEmoji = "🌧️"
    ),
    FOREST(
        title = "Forest Canopy",
        subtitle = "Calm rustling leaves and serene birds",
        iconEmoji = "🌲"
    ),
    ALPHA_WAVES(
        title = "Binaural Alpha (10Hz)",
        subtitle = "Flow state brainwave focus frequencies",
        iconEmoji = "🧠"
    ),
    BROWN_NOISE(
        title = "Deep Brown Noise",
        subtitle = "Warm low-frequency static to mask noise",
        iconEmoji = "📻"
    ),
    WHITE_NOISE(
        title = "Crisp White Noise",
        subtitle = "Full-spectrum acoustic distraction blocker",
        iconEmoji = "💨"
    ),
    CAMPFIRE(
        title = "Cozy Campfire",
        subtitle = "Subtle wood crackle & ambient warmth",
        iconEmoji = "🪵"
    ),
    LOFI_PULSE(
        title = "Lo-Fi Chill Pulse",
        subtitle = "Warm harmonic ambient focus pulses",
        iconEmoji = "🎧"
    )
}
