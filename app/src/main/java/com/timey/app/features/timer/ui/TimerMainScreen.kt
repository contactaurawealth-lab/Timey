package com.timey.app.features.timer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.timey.app.core.audio.SoundscapePlayer
import com.timey.app.core.ui.component.CircularTimerRing
import com.timey.app.core.ui.component.KomodoRoadCard
import com.timey.app.domain.model.PomodoroPhase
import com.timey.app.domain.model.ThemeMode
import com.timey.app.domain.model.TimerMode
import com.timey.app.features.fullscreen.ui.FullscreenFocusScreen
import com.timey.app.features.timer.viewmodel.TimerStatus
import com.timey.app.features.timer.viewmodel.TimerViewModel
import java.util.Locale

@Composable
fun TimerMainScreen(
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme

    // Fullscreen Immersive Mode View
    if (uiState.isFullscreenFocus) {
        FullscreenFocusScreen(
            viewModel = viewModel,
            onExitFullscreen = { viewModel.setFullscreenFocus(false) }
        )
        return
    }

    var showSubjectDialog by remember { mutableStateOf(false) }
    var pinInputValue by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    val isFocusPhase = uiState.pomodoroPhase == PomodoroPhase.FOCUS
    val isRunning = uiState.status == TimerStatus.RUNNING

    val displaySeconds = if (uiState.mode == TimerMode.FLOWMODORO || uiState.mode == TimerMode.STOPWATCH) {
        uiState.elapsedSeconds
    } else {
        uiState.remainingSeconds
    }

    val minutes = displaySeconds / 60
    val seconds = displaySeconds % 60
    val formattedTime = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    val progress = if (uiState.totalDurationSeconds > 0) {
        (uiState.totalDurationSeconds - uiState.remainingSeconds).toFloat() / uiState.totalDurationSeconds.toFloat()
    } else {
        (uiState.elapsedSeconds % 3600).toFloat() / 3600f
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val isWideScreen = maxWidth > 650.dp || maxHeight < 520.dp
        val dynamicRingSize = if (isWideScreen) {
            min(maxHeight * 0.65f, 260.dp)
        } else {
            min(maxWidth * 0.66f, 260.dp)
        }

        if (isWideScreen) {
            // Responsive 2-Column Layout (Landscape & Tablets)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Timer Ring & Primary Action Buttons
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularTimerRing(
                        progress = progress,
                        isFocusMode = isFocusPhase,
                        isRunning = isRunning,
                        size = dynamicRingSize,
                        strokeWidth = 12.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.clickable { showSubjectDialog = true }
                        ) {
                            Text(
                                text = uiState.sessionSubject,
                                color = colors.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = formattedTime,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = colors.onSurface,
                                letterSpacing = (-1).sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.sessionTopic,
                                    color = colors.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Topic",
                                    tint = colors.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    TimerControlsRow(
                        isRunning = isRunning,
                        mode = uiState.mode,
                        onPlayPause = { if (isRunning) viewModel.pauseTimer() else viewModel.startTimer() },
                        onReset = { viewModel.requestResetTimer() },
                        onSkip = { viewModel.skipCurrentPhase() },
                        onFlowBreak = { viewModel.finishFlowmodoroAndCalculateBreak() },
                        onCustomDuration = { viewModel.toggleCustomDurationDialog(true) }
                    )
                }

                // Right Column: Controls, Komodo Companion, Mode Switcher, and Custom Alarm
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TimerTopBar(
                        themeMode = uiState.themeMode,
                        onCycleTheme = { viewModel.cycleThemeMode() },
                        onOpenCustomAlarm = { viewModel.toggleCustomAlarmDialog(true) },
                        onEnterFullscreen = { viewModel.setFullscreenFocus(true) },
                        phase = uiState.pomodoroPhase
                    )

                    KomodoRoadCard(companion = uiState.companion)

                    ModeSelectorBar(
                        currentMode = uiState.mode,
                        isRunning = isRunning,
                        onSelectMode = { viewModel.setTimerMode(it) }
                    )

                    CustomAlarmQuickCard(
                        alarmSoundTitle = uiState.customAudioTitle?.takeIf { uiState.alarmSound == com.timey.app.domain.model.AlarmSound.CUSTOM_USER_AUDIO } ?: uiState.alarmSound.title,
                        volumePercent = (uiState.alarmVolume * 100).toInt(),
                        onClick = { viewModel.toggleCustomAlarmDialog(true) }
                    )
                }
            }
        } else {
            // Responsive Single Column Layout (Portrait Phones)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                TimerTopBar(
                    themeMode = uiState.themeMode,
                    onCycleTheme = { viewModel.cycleThemeMode() },
                    onOpenCustomAlarm = { viewModel.toggleCustomAlarmDialog(true) },
                    onEnterFullscreen = { viewModel.setFullscreenFocus(true) },
                    phase = uiState.pomodoroPhase
                )

                Spacer(modifier = Modifier.height(14.dp))

                KomodoRoadCard(companion = uiState.companion)

                Spacer(modifier = Modifier.height(12.dp))

                ModeSelectorBar(
                    currentMode = uiState.mode,
                    isRunning = isRunning,
                    onSelectMode = { viewModel.setTimerMode(it) }
                )

                // Strict Mode Distraction Banner (if active)
                AnimatedVisibility(
                    visible = uiState.isDistractionWarningActive,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.error.copy(alpha = 0.15f))
                            .border(1.dp, colors.error, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = colors.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Return within ${uiState.distractionGraceSecondsRemaining}s or focus block will be canceled!",
                                color = colors.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Center Circular Timer Ring
                CircularTimerRing(
                    progress = progress,
                    isFocusMode = isFocusPhase,
                    isRunning = isRunning,
                    size = dynamicRingSize,
                    strokeWidth = 12.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.clickable { showSubjectDialog = true }
                    ) {
                        Text(
                            text = uiState.sessionSubject,
                            color = colors.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = formattedTime,
                            fontSize = 50.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = colors.onSurface,
                            letterSpacing = (-1).sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = uiState.sessionTopic,
                                color = colors.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Topic",
                                tint = colors.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Controls Row (Reset, Play/Pause, Skip, Custom Duration)
                TimerControlsRow(
                    isRunning = isRunning,
                    mode = uiState.mode,
                    onPlayPause = { if (isRunning) viewModel.pauseTimer() else viewModel.startTimer() },
                    onReset = { viewModel.requestResetTimer() },
                    onSkip = { viewModel.skipCurrentPhase() },
                    onFlowBreak = { viewModel.finishFlowmodoroAndCalculateBreak() },
                    onCustomDuration = { viewModel.toggleCustomDurationDialog(true) }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Custom Alarm Quick Card
                CustomAlarmQuickCard(
                    alarmSoundTitle = uiState.customAudioTitle?.takeIf { uiState.alarmSound == com.timey.app.domain.model.AlarmSound.CUSTOM_USER_AUDIO } ?: uiState.alarmSound.title,
                    volumePercent = (uiState.alarmVolume * 100).toInt(),
                    onClick = { viewModel.toggleCustomAlarmDialog(true) }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Accidental Termination Confirmation Dialog
    if (uiState.showTerminationConfirmDialog) {
        val elapsedMins = (uiState.elapsedSeconds / 60).coerceAtLeast(0)
        AlertDialog(
            onDismissRequest = { viewModel.dismissTerminationConfirm() },
            title = {
                Text(
                    text = "End Focus Session Early?",
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "You have focused for $elapsedMins minutes. Quitting early will stop this focus block and log it as incomplete.",
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmTermination() },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error)
                ) {
                    Text("Stop & Reset", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissTerminationConfirm() }) {
                    Text("Keep Focusing")
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // PIN Protection Early Termination Dialog
    if (uiState.showTerminationPinDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissTerminationConfirm()
                pinInputValue = ""
                pinError = false
            },
            title = {
                Text(text = "Focus PIN Required", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Session termination is protected by PIN lock. Enter your security PIN:",
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInputValue,
                        onValueChange = {
                            if (it.length <= 8) pinInputValue = it
                            pinError = false
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = pinError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Incorrect PIN",
                            color = colors.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewModel.verifySecurityPin(pinInputValue)) {
                            pinInputValue = ""
                            pinError = false
                            viewModel.confirmTermination()
                        } else {
                            pinError = true
                        }
                    }
                ) {
                    Text("Unlock & Stop")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.dismissTerminationConfirm()
                    pinInputValue = ""
                    pinError = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Subject & Topic Selector Dialog
    if (showSubjectDialog) {
        SubjectSelectionDialog(
            initialSubject = uiState.sessionSubject,
            initialTopic = uiState.sessionTopic,
            viewModel = viewModel,
            onDismiss = { showSubjectDialog = false }
        )
    }

    // Custom Alarm Dialog
    if (uiState.showCustomAlarmDialog) {
        CustomAlarmDialog(
            currentSound = uiState.alarmSound,
            volume = uiState.alarmVolume,
            isPreviewing = uiState.isPreviewingAlarm,
            customAudioTitle = uiState.customAudioTitle,
            hasCustomAudio = uiState.hasCustomAudio,
            viewModel = viewModel,
            onDismiss = { viewModel.toggleCustomAlarmDialog(false) }
        )
    }

    // Custom Duration Dialog
    if (uiState.showCustomDurationDialog) {
        CustomDurationDialog(
            initialMinutes = uiState.totalDurationSeconds / 60,
            viewModel = viewModel,
            onDismiss = { viewModel.toggleCustomDurationDialog(false) }
        )
    }

    // Post-Session Reflection Dialog
    if (uiState.showRetrospectiveDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRetrospective() },
            title = {
                Text(
                    text = "🎉 Session Complete!",
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "You studied ${uiState.lastCompletedMinutes} minutes on Focus Road.",
                        color = colors.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "+${uiState.lastCompletedMinutes * 10} XP awarded to your companion!",
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissRetrospective() },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(text = "Continue", color = colors.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun TimerTopBar(
    themeMode: ThemeMode,
    onCycleTheme: () -> Unit,
    onOpenCustomAlarm: () -> Unit,
    onEnterFullscreen: () -> Unit,
    phase: PomodoroPhase
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⏱️", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Timey",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = phase.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.primary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Fullscreen Mode Launcher
            IconButton(
                onClick = onEnterFullscreen,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Immersive Fullscreen",
                    tint = colors.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Custom Alarm Shortcut
            IconButton(
                onClick = onOpenCustomAlarm,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Custom Alarm",
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Theme Switcher (System / Light / Dark)
            IconButton(
                onClick = onCycleTheme,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant.copy(alpha = 0.6f))
            ) {
                val icon = when (themeMode) {
                    ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                    ThemeMode.LIGHT -> Icons.Default.LightMode
                    ThemeMode.DARK -> Icons.Default.DarkMode
                }
                Icon(
                    imageVector = icon,
                    contentDescription = themeMode.label,
                    tint = colors.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ModeSelectorBar(
    currentMode: TimerMode,
    isRunning: Boolean,
    onSelectMode: (TimerMode) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimerMode.values().forEach { mode ->
            val isSelected = currentMode == mode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) colors.primary else colors.surface)
                    .border(
                        1.dp,
                        if (isSelected) colors.primary else colors.outline.copy(alpha = 0.35f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = !isRunning) { onSelectMode(mode) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = mode.title,
                    color = if (isSelected) colors.onPrimary else colors.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun TimerControlsRow(
    isRunning: Boolean,
    mode: TimerMode,
    onPlayPause: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onFlowBreak: () -> Unit,
    onCustomDuration: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset / Stop Button
        IconButton(
            onClick = onReset,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(colors.surfaceVariant)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset",
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Main Play / Pause Button
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(if (isRunning) colors.secondary else colors.primary)
                .clickable { onPlayPause() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isRunning) "Pause" else "Start",
                tint = colors.onPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Skip Phase Button
        IconButton(
            onClick = onSkip,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(colors.surfaceVariant)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Skip Phase",
                tint = colors.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Context Action Button (Custom Duration or Calculate Flow Break)
        if (mode == TimerMode.FLOWMODORO) {
            IconButton(
                onClick = onFlowBreak,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(colors.tertiary.copy(alpha = 0.2f))
                    .border(1.dp, colors.tertiary, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Calculate Break",
                    tint = colors.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            IconButton(
                onClick = onCustomDuration,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Set Custom Duration",
                    tint = colors.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CustomAlarmQuickCard(
    alarmSoundTitle: String,
    volumePercent: Int,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Alarm",
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Alarm Chime: $alarmSoundTitle",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )
                    Text(
                        text = "Volume: $volumePercent% • Tap to customize tone",
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Change",
                color = colors.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
