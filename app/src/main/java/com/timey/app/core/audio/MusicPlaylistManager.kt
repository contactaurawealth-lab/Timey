package com.timey.app.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class LoopMode {
    NONE,
    REPEAT_ONE,
    REPEAT_ALL
}

data class PlayableTrack(
    val id: String,
    val title: String,
    val artist: String,
    val filePath: String,
    val durationMs: Long
)

class MusicPlaylistManager(private val context: Context) {

    private val TAG = "MusicPlaylistManager"
    private var mediaPlayer: MediaPlayer? = null
    private val musicDir = File(context.filesDir, "study_music").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("timey_playlist", Context.MODE_PRIVATE)

    private val _playlist = MutableStateFlow<List<PlayableTrack>>(emptyList())
    val playlist: StateFlow<List<PlayableTrack>> = _playlist.asStateFlow()

    private val _currentTrack = MutableStateFlow<PlayableTrack?>(null)
    val currentTrack: StateFlow<PlayableTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _loopMode = MutableStateFlow(LoopMode.REPEAT_ALL)
    val loopMode: StateFlow<LoopMode> = _loopMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private var currentVolume: Float = 0.70f

    init {
        loadPersistedPlaylist()
    }

    private fun loadPersistedPlaylist() {
        val jsonString = prefs.getString("tracks_json", null) ?: return
        try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<PlayableTrack>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val file = File(obj.getString("filePath"))
                if (file.exists() && file.length() > 0) {
                    list.add(
                        PlayableTrack(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            artist = obj.optString("artist", "Personal Audio"),
                            filePath = file.absolutePath,
                            durationMs = obj.optLong("durationMs", 0L)
                        )
                    )
                }
            }
            _playlist.value = list
            if (list.isNotEmpty()) {
                _currentTrack.value = list.first()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load persisted tracks: ${e.message}")
        }
    }

    private fun persistPlaylist() {
        try {
            val array = JSONArray()
            _playlist.value.forEach { track ->
                val obj = JSONObject().apply {
                    put("id", track.id)
                    put("title", track.title)
                    put("artist", track.artist)
                    put("filePath", track.filePath)
                    put("durationMs", track.durationMs)
                }
                array.put(obj)
            }
            prefs.edit().putString("tracks_json", array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist tracks: ${e.message}")
        }
    }

    fun importTrackFromUri(uri: Uri): PlayableTrack? {
        return try {
            val contentResolver = context.contentResolver
            val originalName = queryFileName(uri) ?: "Study Track ${System.currentTimeMillis() % 1000}"
            val cleanName = originalName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(musicDir, "${UUID.randomUUID()}_$cleanName")

            contentResolver.openInputStream(uri)?.use { inStream ->
                FileOutputStream(targetFile).use { outStream ->
                    inStream.copyTo(outStream)
                }
            }

            var title = originalName.substringBeforeLast(".")
            var artist = "Personal Track"
            var duration = 0L

            try {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(targetFile.absolutePath)
                val metaTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val metaArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val metaDur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                if (!metaTitle.isNullOrBlank()) title = metaTitle
                if (!metaArtist.isNullOrBlank()) artist = metaArtist
                if (!metaDur.isNullOrBlank()) duration = metaDur.toLongOrNull() ?: 0L
                mmr.release()
            } catch (e: Exception) {
                Log.w(TAG, "Could not extract metadata: ${e.message}")
            }

            val newTrack = PlayableTrack(
                id = UUID.randomUUID().toString(),
                title = title,
                artist = artist,
                filePath = targetFile.absolutePath,
                durationMs = duration
            )

            val updatedList = _playlist.value + newTrack
            _playlist.value = updatedList
            persistPlaylist()

            if (_currentTrack.value == null) {
                _currentTrack.value = newTrack
            }
            newTrack
        } catch (e: Exception) {
            Log.e(TAG, "Error importing audio: ${e.message}", e)
            null
        }
    }

    fun playTrack(track: PlayableTrack) {
        // Stop any ambient soundscape if playing personal music
        SoundscapePlayer.stop()

        stop()
        _currentTrack.value = track

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(track.filePath)
                setVolume(currentVolume, currentVolume)
                prepare()
                start()
            }

            mediaPlayer = mp
            _isPlaying.value = true

            mp.setOnCompletionListener {
                handleTrackCompletion()
            }

            mp.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                _isPlaying.value = false
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play track: ${e.message}", e)
            _isPlaying.value = false
        }
    }

    fun resume() {
        val current = _currentTrack.value ?: _playlist.value.firstOrNull() ?: return
        if (mediaPlayer == null) {
            playTrack(current)
        } else {
            mediaPlayer?.start()
            _isPlaying.value = true
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            _isPlaying.value = false
        } catch (_: Exception) {}
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    fun nextTrack() {
        val tracks = _playlist.value
        if (tracks.isEmpty()) return
        val current = _currentTrack.value
        val currentIndex = tracks.indexOfFirst { it.id == current?.id }

        val nextIndex = if (_isShuffle.value) {
            tracks.indices.random()
        } else {
            (currentIndex + 1) % tracks.size
        }

        playTrack(tracks[nextIndex])
    }

    fun previousTrack() {
        val tracks = _playlist.value
        if (tracks.isEmpty()) return
        val current = _currentTrack.value
        val currentIndex = tracks.indexOfFirst { it.id == current?.id }

        val prevIndex = if (currentIndex <= 0) tracks.size - 1 else currentIndex - 1
        playTrack(tracks[prevIndex])
    }

    private fun handleTrackCompletion() {
        when (_loopMode.value) {
            LoopMode.REPEAT_ONE -> {
                _currentTrack.value?.let { playTrack(it) }
            }
            LoopMode.REPEAT_ALL -> {
                nextTrack()
            }
            LoopMode.NONE -> {
                val tracks = _playlist.value
                val currentIndex = tracks.indexOfFirst { it.id == _currentTrack.value?.id }
                if (currentIndex >= 0 && currentIndex < tracks.size - 1) {
                    nextTrack()
                } else {
                    _isPlaying.value = false
                }
            }
        }
    }

    fun cycleLoopMode() {
        _loopMode.value = when (_loopMode.value) {
            LoopMode.REPEAT_ALL -> LoopMode.REPEAT_ONE
            LoopMode.REPEAT_ONE -> LoopMode.NONE
            LoopMode.NONE -> LoopMode.REPEAT_ALL
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(currentVolume, currentVolume)
        } catch (_: Exception) {}
    }

    fun deleteTrack(trackId: String) {
        val track = _playlist.value.find { it.id == trackId } ?: return
        if (_currentTrack.value?.id == trackId) {
            stop()
            _currentTrack.value = null
        }
        try {
            File(track.filePath).delete()
        } catch (_: Exception) {}

        _playlist.value = _playlist.value.filter { it.id != trackId }
        persistPlaylist()
        if (_currentTrack.value == null && _playlist.value.isNotEmpty()) {
            _currentTrack.value = _playlist.value.first()
        }
    }

    fun stop() {
        _isPlaying.value = false
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    private fun queryFileName(uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) name = cursor.getString(idx)
                }
            }
        } catch (_: Exception) {}
        return name ?: uri.lastPathSegment
    }
}
