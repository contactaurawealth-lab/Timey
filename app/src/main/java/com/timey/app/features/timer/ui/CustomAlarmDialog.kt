package com.timey.app.features.timer.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timey.app.domain.model.AlarmSound
import com.timey.app.features.timer.viewmodel.TimerViewModel

@Composable
fun CustomAlarmDialog(
    currentSound: AlarmSound,
    volume: Float,
    isPreviewing: Boolean,
    customAudioTitle: String?,
    hasCustomAudio: Boolean,
    viewModel: TimerViewModel,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    // File picker launcher for custom user audio (.mp3, .wav, .m4a, .ogg)
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importCustomAudio(it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Custom Alarm",
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Custom Alarm & Audio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colors.onSurface
                    )
                    Text(
                        text = "Import custom ringtones or select chimes",
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Volume Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Volume",
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Alarm Volume",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurface
                        )
                    }
                    Text(
                        text = "${(volume * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }

                Slider(
                    value = volume,
                    onValueChange = { viewModel.setAlarmVolume(it) },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primary,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = colors.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // --- CUSTOM USER IMPORTED AUDIO SECTION ---
                Text(
                    text = "YOUR CUSTOM AUDIO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (hasCustomAudio && customAudioTitle != null) {
                    val isCustomActive = currentSound == AlarmSound.CUSTOM_USER_AUDIO
                    val isCurrentPreviewing = isCustomActive && isPreviewing

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCustomActive) colors.primary.copy(alpha = 0.15f) else colors.surfaceVariant.copy(alpha = 0.5f))
                            .border(
                                width = if (isCustomActive) 1.5.dp else 1.dp,
                                color = if (isCustomActive) colors.primary else colors.outline.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { viewModel.setAlarmSound(AlarmSound.CUSTOM_USER_AUDIO) }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = "Audio",
                                    tint = if (isCustomActive) colors.primary else colors.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = customAudioTitle,
                                            fontWeight = if (isCustomActive) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = if (isCustomActive) colors.primary else colors.onSurface,
                                            maxLines = 1
                                        )
                                        if (isCustomActive) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Active",
                                                tint = colors.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Imported audio file",
                                        fontSize = 11.sp,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (isCurrentPreviewing) {
                                            viewModel.stopAlarmPreview()
                                        } else {
                                            viewModel.previewAlarmSound(AlarmSound.CUSTOM_USER_AUDIO)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrentPreviewing) colors.error else colors.primary.copy(alpha = 0.2f))
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "Test Audio",
                                        tint = if (isCurrentPreviewing) colors.onPrimary else colors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = { audioPickerLauncher.launch("audio/*") },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(colors.surfaceVariant)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = "Change File",
                                        tint = colors.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Import Audio Prompt Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surfaceVariant.copy(alpha = 0.4f))
                            .border(
                                width = 1.dp,
                                color = colors.primary.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { audioPickerLauncher.launch("audio/*") }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = "Add Audio",
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Import Custom Audio (.mp3, .wav, .m4a)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // --- PROCEDURAL & SYSTEM CHIMES SECTION ---
                Text(
                    text = "BUILT-IN ZEN CHIMES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Sound choices list (excluding CUSTOM_USER_AUDIO since handled in top card)
                AlarmSound.values().filter { it != AlarmSound.CUSTOM_USER_AUDIO }.forEach { sound ->
                    val isSelected = sound == currentSound
                    val isCurrentPreviewing = isSelected && isPreviewing

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.surfaceVariant.copy(alpha = 0.5f))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) colors.primary else colors.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { viewModel.setAlarmSound(sound) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = sound.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) colors.primary else colors.onSurface
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = colors.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = sound.description,
                                    fontSize = 11.sp,
                                    color = colors.onSurfaceVariant
                                )
                            }

                            if (!sound.isSilent) {
                                IconButton(
                                    onClick = {
                                        if (isCurrentPreviewing) {
                                            viewModel.stopAlarmPreview()
                                        } else {
                                            viewModel.previewAlarmSound(sound)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrentPreviewing) colors.error else colors.primary.copy(alpha = 0.2f))
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "Test Chime",
                                        tint = if (isCurrentPreviewing) colors.onPrimary else colors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text(
                    text = "Save Chime",
                    color = colors.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(24.dp)
    )
}
