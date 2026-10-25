package com.timey.app.features.fullscreen.ui

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.timey.app.core.ui.component.CircularTimerRing
import com.timey.app.domain.model.PomodoroPhase
import com.timey.app.domain.model.TimerMode
import com.timey.app.features.timer.viewmodel.TimerStatus
import com.timey.app.features.timer.viewmodel.TimerViewModel
import java.util.Locale

@Composable
fun FullscreenFocusScreen(
    viewModel: TimerViewModel,
    onExitFullscreen: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme

    // Motivational Focus Quotes
    val quotes = remember {
        listOf(
            "Deep work is the superpower of the 21st century.",
            "Focus is a muscle: the more you practice, the stronger it gets.",
            "Don't count the seconds, make the seconds count.",
            "Small daily disciplines compound into massive breakthroughs.",
            "You don't need motivation when you have momentum."
        )
    }
    val currentQuote = remember { quotes.random() }

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Immersive Mode: Hide Status Bar and Navigation Bar
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            val window = (context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp)
    ) {
        // Exit Fullscreen Button (Top Right)
        IconButton(
            onClick = {
                if (uiState.pinProtectionEnabled && isRunning) {
                    showPinDialog = true
                } else {
                    onExitFullscreen()
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(42.dp)
                .clip(CircleShape)
                .background(colors.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = if (uiState.pinProtectionEnabled && isRunning) Icons.Default.Lock else Icons.Default.Close,
                contentDescription = "Exit Fullscreen",
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        // Subject / Topic Header (Top Center)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = uiState.sessionSubject.uppercase(),
                fontSize = 13.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
            Text(
                text = uiState.sessionTopic,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface
            )
        }

        // Center Big Minimal Ring
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularTimerRing(
                progress = progress,
                isFocusMode = uiState.pomodoroPhase == PomodoroPhase.FOCUS,
                isRunning = isRunning,
                size = 280.dp,
                strokeWidth = 14.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.onSurface,
                        letterSpacing = (-2).sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = uiState.pomodoroPhase.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Ambient Quote
            Text(
                text = "“$currentQuote”",
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Minimal Controls (Play/Pause, Skip)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Play / Pause
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) colors.secondary else colors.primary)
                        .clickable {
                            if (isRunning) viewModel.pauseTimer() else viewModel.startTimer()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Play",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Skip Phase
                IconButton(
                    onClick = { viewModel.skipCurrentPhase() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip Phase",
                        tint = colors.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Bottom hint
        Text(
            text = if (uiState.pinProtectionEnabled) "🔒 PIN protection active" else "Swipe from edge to show system bars",
            fontSize = 11.sp,
            color = colors.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showPinDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                pinInput = ""
                pinError = false
            },
            title = {
                Text(text = "Focus PIN Required", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Enter your security PIN to exit immersive mode:",
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 8) pinInput = it
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
                        if (viewModel.verifySecurityPin(pinInput)) {
                            showPinDialog = false
                            onExitFullscreen()
                        } else {
                            pinError = true
                        }
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
