package com.timey.app.features.music.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timey.app.core.audio.LoopMode
import com.timey.app.core.audio.PlayableTrack
import com.timey.app.core.audio.SoundscapePlayer
import com.timey.app.core.audio.SoundscapeType
import com.timey.app.features.timer.viewmodel.TimerViewModel

@Composable
fun MusicScreen(
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val soundscapeIsPlaying by SoundscapePlayer.isPlaying.collectAsState()
    val activeSoundscape by SoundscapePlayer.currentSoundscape.collectAsState()

    val playlist by viewModel.musicPlaylistManager.playlist.collectAsState()
    val currentTrack by viewModel.musicPlaylistManager.currentTrack.collectAsState()
    val isMusicPlaying by viewModel.musicPlaylistManager.isPlaying.collectAsState()
    val loopMode by viewModel.musicPlaylistManager.loopMode.collectAsState()
    val isShuffle by viewModel.musicPlaylistManager.isShuffle.collectAsState()

    // File launcher for personal MP3s
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.musicPlaylistManager.importTrackFromUri(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Screen Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Study Audio",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )
                Text(
                    text = "Acoustic soundscapes & personal study tracks",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant
                )
            }

            if (selectedTab == 1) {
                Button(
                    onClick = { audioPicker.launch("audio/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Track", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Import MP3", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tab Selector (Ambient Soundscapes vs Personal MP3)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = colors.surface,
            contentColor = colors.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Ambient Soundscapes", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("My Playlist (${playlist.size})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Independent Volume Sliders
        VolumeControlsCard(
            musicVolume = uiState.musicVolume,
            alarmVolume = uiState.alarmVolume,
            onMusicVolumeChange = { viewModel.setMusicVolume(it) },
            onAlarmVolumeChange = { viewModel.setAlarmVolume(it) },
            onPreviewAlarm = { viewModel.previewCurrentAlarmSound() }
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 0) {
            // Ambient Soundscapes
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(SoundscapeType.values()) { soundscape ->
                    val isCurrent = activeSoundscape == soundscape && soundscapeIsPlaying
                    SoundscapeItemCard(
                        soundscape = soundscape,
                        isPlaying = isCurrent,
                        onToggle = {
                            if (isCurrent) {
                                SoundscapePlayer.stop()
                            } else {
                                viewModel.musicPlaylistManager.pause()
                                SoundscapePlayer.playSoundscape(soundscape, uiState.musicVolume)
                            }
                        }
                    )
                }
            }
        } else {
            // Personal Playlist
            Column(modifier = Modifier.weight(1f)) {
                // Now Playing Mini-Bar
                if (currentTrack != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentTrack!!.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = currentTrack!!.artist,
                                        fontSize = 12.sp,
                                        color = colors.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Controls Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.musicPlaylistManager.toggleShuffle() }) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = if (isShuffle) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(onClick = { viewModel.musicPlaylistManager.previousTrack() }) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "Previous",
                                        tint = colors.onSurface,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary)
                                        .clickable { viewModel.musicPlaylistManager.togglePlayPause() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isMusicPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isMusicPlaying) "Pause" else "Play",
                                        tint = colors.onPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                IconButton(onClick = { viewModel.musicPlaylistManager.nextTrack() }) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Next",
                                        tint = colors.onSurface,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                IconButton(onClick = { viewModel.musicPlaylistManager.cycleLoopMode() }) {
                                    val icon = when (loopMode) {
                                        LoopMode.REPEAT_ALL -> Icons.Default.Repeat
                                        LoopMode.REPEAT_ONE -> Icons.Default.RepeatOne
                                        LoopMode.NONE -> Icons.Default.Repeat
                                    }
                                    val alpha = if (loopMode == LoopMode.NONE) 0.5f else 1.0f
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = "Loop Mode",
                                        tint = if (loopMode == LoopMode.NONE) colors.onSurfaceVariant.copy(alpha = alpha) else colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (playlist.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No personal tracks imported yet",
                                fontSize = 14.sp,
                                color = colors.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap 'Import MP3' to add study music or lo-fi audio.",
                                fontSize = 12.sp,
                                color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(playlist) { track ->
                            val isSelected = currentTrack?.id == track.id
                            PlayableTrackItem(
                                track = track,
                                isSelected = isSelected,
                                isPlaying = isSelected && isMusicPlaying,
                                onPlay = { viewModel.musicPlaylistManager.playTrack(track) },
                                onDelete = { viewModel.musicPlaylistManager.deleteTrack(track.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VolumeControlsCard(
    musicVolume: Float,
    alarmVolume: Float,
    onMusicVolumeChange: (Float) -> Unit,
    onAlarmVolumeChange: (Float) -> Unit,
    onPreviewAlarm: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Music Volume
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Music Volume",
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Study Audio Volume: ${(musicVolume * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
            }
            Slider(
                value = musicVolume,
                onValueChange = onMusicVolumeChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.primary,
                    activeTrackColor = colors.primary
                ),
                modifier = Modifier.height(28.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Alarm Volume
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Alarm Volume",
                        tint = colors.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Completion Chime Volume: ${(alarmVolume * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )
                }

                Text(
                    text = "Preview",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.secondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onPreviewAlarm() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Slider(
                value = alarmVolume,
                onValueChange = onAlarmVolumeChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.secondary,
                    activeTrackColor = colors.secondary
                ),
                modifier = Modifier.height(28.dp)
            )
        }
    }
}

@Composable
fun SoundscapeItemCard(
    soundscape: SoundscapeType,
    isPlaying: Boolean,
    onToggle: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isPlaying) colors.primary.copy(alpha = 0.12f) else colors.surface)
            .border(
                1.dp,
                if (isPlaying) colors.primary else colors.outline.copy(alpha = 0.25f),
                RoundedCornerShape(14.dp)
            )
            .clickable { onToggle() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = soundscape.iconEmoji, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = soundscape.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPlaying) colors.primary else colors.onSurface
                    )
                    Text(
                        text = soundscape.subtitle,
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) colors.primary else colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isPlaying) colors.onPrimary else colors.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun PlayableTrackItem(
    track: PlayableTrack,
    isSelected: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) colors.primary.copy(alpha = 0.1f) else colors.surface)
            .border(
                1.dp,
                if (isSelected) colors.primary else colors.outline.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onPlay() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isSelected) colors.primary else colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = track.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) colors.primary else colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Track",
                    tint = colors.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
