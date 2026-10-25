package com.timey.app

import com.timey.app.core.audio.AudioTrackGenerator
import com.timey.app.core.audio.SoundscapeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioGeneratorTest {

    @Test
    fun testProceduralAudioGenerationForAllSoundscapes() {
        val sampleRate = 44100
        val durationSec = 2
        val expectedLength = sampleRate * durationSec

        for (type in SoundscapeType.values()) {
            val buffer = AudioTrackGenerator.generateBuffer(type, durationSeconds = durationSec)
            assertEquals("Buffer length mismatch for $type", expectedLength, buffer.size)

            // Ensure buffer contains actual audio samples (not pure zeros)
            val hasNonZero = buffer.any { it != 0.toShort() }
            assertTrue("Buffer for $type is completely silent", hasNonZero)
        }
    }
}
