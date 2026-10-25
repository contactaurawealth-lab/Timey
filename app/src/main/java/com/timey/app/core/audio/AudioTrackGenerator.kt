package com.timey.app.core.audio

import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

object AudioTrackGenerator {

    private const val SAMPLE_RATE = 44100
    private val random = Random(42)

    /**
     * Generates a loopable 16-bit PCM mono buffer for ambient soundscapes.
     */
    fun generateBuffer(type: SoundscapeType, durationSeconds: Int = 4): ShortArray {
        val totalSamples = SAMPLE_RATE * durationSeconds
        val buffer = ShortArray(totalSamples)

        when (type) {
            SoundscapeType.RAIN -> {
                // Pink-ish filtered noise with dynamic raindrop modulation
                var b0 = 0.0
                var b1 = 0.0
                var b2 = 0.0
                var b3 = 0.0
                for (i in 0 until totalSamples) {
                    val white = random.nextDouble() * 2.0 - 1.0
                    b0 = 0.99886 * b0 + white * 0.0555179
                    b1 = 0.99332 * b1 + white * 0.0750759
                    b2 = 0.96900 * b2 + white * 0.1538520
                    b3 = 0.86650 * b3 + white * 0.3104856
                    val pink = (b0 + b1 + b2 + b3) * 0.25

                    // Slow swell modulation (distant rain gusts)
                    val swell = 0.8 + 0.2 * sin(2.0 * PI * i / (SAMPLE_RATE * 2.5))
                    val sample = (pink * swell * 0.7 * Short.MAX_VALUE).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.BROWN_NOISE -> {
                // Integrated white noise (Brownian / red noise)
                var lastSample = 0.0
                for (i in 0 until totalSamples) {
                    val white = random.nextDouble() * 2.0 - 1.0
                    lastSample = (lastSample + (0.02 * white)) / 1.02
                    val sample = (lastSample * 3.5 * Short.MAX_VALUE).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.WHITE_NOISE -> {
                // Pure Gaussian white noise
                for (i in 0 until totalSamples) {
                    val white = (random.nextDouble() * 2.0 - 1.0) * 0.3
                    val sample = (white * Short.MAX_VALUE).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.ALPHA_WAVES -> {
                // Binaural-style soothing 216Hz carrier with 10Hz alpha difference beat
                val baseFreq = 216.0
                val beatFreq = 10.0
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val carrier = sin(2.0 * PI * baseFreq * t)
                    val modulation = 0.6 + 0.4 * sin(2.0 * PI * beatFreq * t)
                    // Sub harmonic warmth (108Hz)
                    val warmSub = 0.3 * sin(2.0 * PI * (baseFreq / 2) * t)

                    val value = (carrier * modulation * 0.6 + warmSub) * 0.5
                    val sample = (value * Short.MAX_VALUE).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.FOREST -> {
                // Low gentle wind sweep + sporadic soft chirps
                var windFilter = 0.0
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val white = random.nextDouble() * 2.0 - 1.0
                    windFilter = 0.98 * windFilter + 0.02 * white
                    val windSwell = 0.5 + 0.5 * sin(2.0 * PI * 0.25 * t)
                    var valOut = windFilter * windSwell * 1.5

                    // Occasional chirping pulse
                    val chirpPhase = (t * 3.0) % 3.0
                    if (chirpPhase < 0.15) {
                        val chirpFreq = 2800.0 + 800.0 * sin(2.0 * PI * 8.0 * chirpPhase)
                        val chirp = sin(2.0 * PI * chirpFreq * t) * (1.0 - chirpPhase / 0.15) * 0.15
                        valOut += chirp
                    }

                    val sample = (valOut * 0.6 * Short.MAX_VALUE).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.CAMPFIRE -> {
                // Brown base + random crackle pops
                var brown = 0.0
                for (i in 0 until totalSamples) {
                    val white = random.nextDouble() * 2.0 - 1.0
                    brown = (brown + (0.015 * white)) / 1.015
                    var pop = 0.0
                    if (random.nextDouble() < 0.0008) {
                        pop = (random.nextDouble() * 2.0 - 1.0) * 0.7
                    }
                    val total = (brown * 2.5 + pop) * 0.6
                    val sample = (total * Short.MAX_VALUE).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }

            SoundscapeType.LOFI_PULSE -> {
                // Chill Rhodes-style warm minor chords with slow tremolo
                val freqA = 220.0 // A3
                val freqC = 261.63 // C4
                val freqE = 329.63 // E4
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val pulse = 0.5 + 0.5 * sin(2.0 * PI * 1.2 * t)
                    val chord = sin(2.0 * PI * freqA * t) * 0.4 +
                               sin(2.0 * PI * freqC * t) * 0.3 +
                               sin(2.0 * PI * freqE * t) * 0.3
                    val warmth = chord * pulse * 0.45
                    val sample = (warmth * Short.MAX_VALUE).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
        }

        // Apply smooth fade-in and fade-out at borders to eliminate looping clicks
        val fadeSamples = SAMPLE_RATE / 20 // 50ms fade
        for (i in 0 until fadeSamples) {
            val factor = i.toFloat() / fadeSamples
            buffer[i] = (buffer[i] * factor).toInt().toShort()
            val endIdx = totalSamples - 1 - i
            buffer[endIdx] = (buffer[endIdx] * factor).toInt().toShort()
        }

        return buffer
    }
}
