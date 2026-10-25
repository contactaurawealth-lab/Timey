package com.timey.app.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object SoundscapePlayer {

    private const val TAG = "SoundscapePlayer"
    private const val SAMPLE_RATE = 44100
    private var audioTrack: AudioTrack? = null
    private var playerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSoundscape = MutableStateFlow<SoundscapeType?>(null)
    val currentSoundscape: StateFlow<SoundscapeType?> = _currentSoundscape.asStateFlow()

    private var currentVolume: Float = 0.70f

    fun playSoundscape(type: SoundscapeType, volume: Float = currentVolume) {
        currentVolume = volume.coerceIn(0f, 1f)
        stop()

        _currentSoundscape.value = type
        _isPlaying.value = true

        playerJob = scope.launch {
            try {
                val samples = AudioTrackGenerator.generateBuffer(type, durationSeconds = 4)
                val bufferSize = samples.size * 2 // 2 bytes per 16-bit short

                val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                    )
                }

                audioTrack = track
                setVolume(currentVolume)
                track.play()

                while (isActive && _isPlaying.value) {
                    track.write(samples, 0, samples.size)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error streaming soundscape: ${e.message}", e)
            } finally {
                releaseTrack()
            }
        }
    }

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        audioTrack?.let { track ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    track.setVolume(currentVolume)
                } else {
                    @Suppress("DEPRECATION")
                    track.setStereoVolume(currentVolume, currentVolume)
                }
            } catch (_: Exception) {}
        }
    }

    fun stop() {
        _isPlaying.value = false
        playerJob?.cancel()
        playerJob = null
        releaseTrack()
    }

    private fun releaseTrack() {
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
