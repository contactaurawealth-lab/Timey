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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timey.app.core.ui.component.CircularTimerRing
import com.timey.app.core.ui.component.KomodoRoadCard
import com.timey.app.core.ui.theme.CalmingSky
import com.timey.app.core.ui.theme.EmeraldDragon
import com.timey.app.core.ui.theme.FieryAmber
import com.timey.app.core.ui.theme.FireRed
import com.timey.app.core.ui.theme.KomodoBorder
import com.timey.app.core.ui.theme.KomodoObsidian
import com.timey.app.core.ui.theme.KomodoSurface
import com.timey.app.core.ui.theme.KomodoSurfaceVariant
import com.timey.app.core.ui.theme.TextMuted
import com.timey.app.core.ui.theme.TextWhite
import com.timey.app.domain.model.PomodoroPhase
import com.timey.app.domain.model.TimerMode
import com.timey.app.features.timer.viewmodel.TimerStatus
import com.timey.app.features.timer.viewmodel.TimerViewModel
import java.util.Locale

@Composable
fun TimerMainScreen(
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KomodoObsidian)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // App Title & Tagline
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Timey",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "Gamified Study & Focus Companion",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            // Phase indicator badge
            val badgeColor = when (uiState.pomodoroPhase) {
                PomodoroPhase.FOCUS -> EmeraldDragon
                PomodoroPhase.SHORT_BREAK -> CalmingSky
                PomodoroPhase.LONG_BREAK -> FieryAmber
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = uiState.pomodoroPhase.label.uppercase(),
                    color = badgeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Komodo Road Progression Card
        KomodoRoadCard(companion = uiState.companion)

        Spacer(modifier = Modifier.height(18.dp))

        // Mode Selector Bar (Pomodoro, Ultradian, Flowmodoro, Exam)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimerMode.values().forEach { mode ->
                val isSelected = uiState.mode == mode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) EmeraldDragon else KomodoSurface)
                        .border(
                            1.dp,
                            if (isSelected) EmeraldDragon else KomodoBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable(enabled = !isRunning) {
                            viewModel.setTimerMode(mode)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = mode.title,
                        color = if (isSelected) KomodoObsidian else TextWhite,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Strict Mode Distraction Banner (if active)
        AnimatedVisibility(
            visible = uiState.isDistractionWarningActive,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FireRed.copy(alpha = 0.2f))
                    .border(1.dp, FireRed, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = FireRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Stay on Komodo Road!",
                            color = FireRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Return within ${uiState.distractionGraceSecondsRemaining}s or session will fail.",
                            color = TextWhite,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Center Circular Timer Ring
        CircularTimerRing(
            progress = progress,
            isFocusMode = isFocusPhase,
            isRunning = isRunning,
            size = 280.dp,
            strokeWidth = 14.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.sessionSubject,
                    color = EmeraldDragon,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = formattedTime,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextWhite,
                    letterSpacing = (-1).sp
                )

                Text(
                    text = uiState.sessionTopic,
                    color = TextMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Controls Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset Button
            IconButton(
                onClick = { viewModel.resetTimer() },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(KomodoSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Main Play / Pause Button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) FieryAmber else EmeraldDragon)
                    .clickable {
                        if (isRunning) viewModel.pauseTimer() else viewModel.startTimer()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Start",
                    tint = KomodoObsidian,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Action: Flow Break OR Add +5 Min
            if (uiState.mode == TimerMode.FLOWMODORO) {
                IconButton(
                    onClick = { viewModel.finishFlowmodoroAndCalculateBreak() },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CalmingSky.copy(alpha = 0.2f))
                        .border(1.dp, CalmingSky, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Take Break",
                        tint = CalmingSky,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = { viewModel.setDurationMinutes((uiState.totalDurationSeconds / 60) + 5) },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(KomodoSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "+5 Min",
                        tint = TextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // Post-Session Reflection Dialog
    if (uiState.showRetrospectiveDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRetrospective() },
            title = {
                Text(
                    text = "🎉 Milestone Reached!",
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            },
            text = {
                Column {
                    Text(
                        text = "You completed ${uiState.lastCompletedMinutes} minutes of focus on Komodo Road.",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "+${uiState.lastCompletedMinutes * 10} XP earned for your dragon companion!",
                        color = EmeraldDragon,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissRetrospective() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDragon)
                ) {
                    Text(text = "Continue Journey", color = KomodoObsidian, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = KomodoSurface
        )
    }
}
