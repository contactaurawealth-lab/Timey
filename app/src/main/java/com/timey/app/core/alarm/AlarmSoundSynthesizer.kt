package com.timey.app.core.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.RingtoneManager
import android.util.Log
import com.timey.app.domain.model.AlarmSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object AlarmSoundSynthesizer {

    private const val TAG = "AlarmSoundSynthesizer"
    private const val SAMPLE_RATE = 44100
    private var previewJob: Job? = null
    private var currentTrack: AudioTrack? = null
    private var mediaPlayer: android.media.MediaPlayer? = null

    fun playAlarmSound(context: Context, sound: AlarmSound, volume: Float = 0.85f) {
        if (sound.isSilent) return

        if (sound == AlarmSound.CUSTOM_USER_AUDIO) {
            playCustomUserAudio(context, volume)
            return
        }

        if (sound == AlarmSound.SYSTEM_DEFAULT) {
            playSystemDefaultRingtone(context)
            return
        }

        stop()

        previewJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                val samples = generateSamplesForSound(sound)
                playPcmSamples(samples, volume)
            } catch (e: Exception) {
                Log.e(TAG, "Error playing procedural alarm: ${e.message}")
            }
        }
    }

    private fun playCustomUserAudio(context: Context, volume: Float) {
        val audioFile = CustomAudioStorage.getStoredCustomAudioFile(context)
        if (audioFile == null) {
            playAlarmSound(context, AlarmSound.GENTLE_CHIME, volume)
            return
        }

        try {
            stop()
            val mp = android.media.MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(audioFile.absolutePath)
                setVolume(volume, volume)
                prepare()
                start()
            }
            mediaPlayer = mp
            mp.setOnCompletionListener {
                stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play custom user audio: ${e.message}", e)
        }
    }

    fun stop() {
        previewJob?.cancel()
        previewJob = null
        try {
            currentTrack?.stop()
            currentTrack?.release()
        } catch (_: Exception) {}
        currentTrack = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    private fun playSystemDefaultRingtone(context: Context) {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, alertUri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play system ringtone: ${e.message}")
        }
    }

    private fun generateSamplesForSound(sound: AlarmSound): ShortArray {
        return when (sound) {
            AlarmSound.ZEN_BELL -> generateZenBell(durationSec = 2.5f)
            AlarmSound.GENTLE_CHIME -> generateGentleChime()
            AlarmSound.DIGITAL_PULSE -> generateDigitalPulse()
            AlarmSound.MORNING_BIRDS -> generateMorningSweep()
            else -> generateGentleChime()
        }
    }

    private fun generateZenBell(durationSec: Float): ShortArray {
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        val f1 = 528.0 // Solfeggio frequency for transformation/peace
        val f2 = 1056.0 // Harmonic octave

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-t * 1.5) // Gentle acoustic decay
            val wave = (sin(2.0 * PI * f1 * t) * 0.7 + sin(2.0 * PI * f2 * t) * 0.3) * envelope
            buffer[i] = (wave * Short.MAX_VALUE * 0.85).toInt().toShort()
        }
        return buffer
    }

    private fun generateGentleChime(): ShortArray {
        // 4 notes arpeggio: A4 (440Hz), C#5 (554.37Hz), E5 (659.25Hz), A5 (880Hz)
        val noteFrequencies = doubleArrayOf(440.0, 554.37, 659.25, 880.0)
        val noteDuration = 0.5f
        val noteSamples = (SAMPLE_RATE * noteDuration).toInt()
        val totalSamples = noteSamples * noteFrequencies.size
        val buffer = ShortArray(totalSamples)

        for ((nIndex, freq) in noteFrequencies.withIndex()) {
            val offset = nIndex * noteSamples
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val envelope = exp(-t * 4.0)
                val wave = sin(2.0 * PI * freq * t) * envelope
                buffer[offset + i] = (wave * Short.MAX_VALUE * 0.8).toInt().toShort()
            }
        }
        return buffer
    }

    private fun generateDigitalPulse(): ShortArray {
        // Two crisp 880Hz beeps
        val beepDuration = 0.12f
        val silenceDuration = 0.08f
        val beepSamples = (SAMPLE_RATE * beepDuration).toInt()
        val silenceSamples = (SAMPLE_RATE * silenceDuration).toInt()
        val totalSamples = (beepSamples * 2) + silenceSamples
        val buffer = ShortArray(totalSamples)

        val freq = 880.0
        // Beep 1
        for (i in 0 until beepSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val wave = sin(2.0 * PI * freq * t)
            buffer[i] = (wave * Short.MAX_VALUE * 0.75).toInt().toShort()
        }
        // Beep 2
        val secondBeepStart = beepSamples + silenceSamples
        for (i in 0 until beepSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val wave = sin(2.0 * PI * freq * t)
            buffer[secondBeepStart + i] = (wave * Short.MAX_VALUE * 0.75).toInt().toShort()
        }
        return buffer
    }

    private fun generateMorningSweep(): ShortArray {
        val duration = 1.8f
        val totalSamples = (SAMPLE_RATE * duration).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 587.33 + sin(2.0 * PI * 2.0 * t) * 120.0
            val envelope = (sin(PI * (t / duration))).coerceAtLeast(0.0)
            val wave = sin(2.0 * PI * freq * t) * envelope
            buffer[i] = (wave * Short.MAX_VALUE * 0.7).toInt().toShort()
        }
        return buffer
    }

    private fun playPcmSamples(samples: ShortArray, volume: Float) {
        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(samples.size * 2)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(samples, 0, samples.size)
        audioTrack.setVolume(volume.coerceIn(0.0f, 1.0f))
        currentTrack = audioTrack
        audioTrack.play()
    }
}
