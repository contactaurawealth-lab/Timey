package com.timey.app.features.settings.ui

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timey.app.core.ui.theme.AccentPalettes
import com.timey.app.domain.model.AlarmSound
import com.timey.app.domain.model.ThemeMode
import com.timey.app.features.timer.viewmodel.TimerViewModel

@Composable
fun SettingsScreen(
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsState()

    // File picker for custom completion ringtone
    val ringtonePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importCustomAudio(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Settings & Preferences",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )
        Text(
            text = "Customize study cycles, ringtones, colors, and automation",
            fontSize = 12.sp,
            color = colors.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Timer Intervals
            item {
                SettingsSectionCard(title = "Timer Intervals & Cycles", icon = Icons.Default.Timer) {
                    // Pomodoro Focus Duration
                    IntervalSliderItem(
                        title = "Pomodoro Focus Block",
                        value = "${uiState.pomodoroStudyMinutes} min",
                        currentVal = uiState.pomodoroStudyMinutes.toFloat(),
                        range = 5f..120f,
                        step = 5,
                        onValueChange = { viewModel.setPomodoroStudyMinutes(it.toInt()) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Short Break Duration
                    IntervalSliderItem(
                        title = "Short Recovery Break",
                        value = "${uiState.pomodoroShortBreakMinutes} min",
                        currentVal = uiState.pomodoroShortBreakMinutes.toFloat(),
                        range = 1f..30f,
                        step = 1,
                        onValueChange = { viewModel.setPomodoroShortBreakMinutes(it.toInt()) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Long Break Duration
                    IntervalSliderItem(
                        title = "Deep Long Recovery",
                        value = "${uiState.pomodoroLongBreakMinutes} min",
                        currentVal = uiState.pomodoroLongBreakMinutes.toFloat(),
                        range = 5f..60f,
                        step = 5,
                        onValueChange = { viewModel.setPomodoroLongBreakMinutes(it.toInt()) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 135:15 Deep Work Duration
                    IntervalSliderItem(
                        title = "135:15 Deep Work Focus",
                        value = "${uiState.deepWorkStudyMinutes} min",
                        currentVal = uiState.deepWorkStudyMinutes.toFloat(),
                        range = 60f..180f,
                        step = 15,
                        onValueChange = { viewModel.setDeepWorkStudyMinutes(it.toInt()) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cycles Before Long Break
                    IntervalSliderItem(
                        title = "Cycles before Deep Long Break",
                        value = "${uiState.targetCyclesBeforeLongBreak} cycles",
                        currentVal = uiState.targetCyclesBeforeLongBreak.toFloat(),
                        range = 2f..8f,
                        step = 1,
                        onValueChange = { viewModel.setTargetCyclesBeforeLongBreak(it.toInt()) }
                    )
                }
            }

            // Section: Automation & Display
            item {
                SettingsSectionCard(title = "Focus Flow & Automation", icon = Icons.Default.Shield) {
                    SwitchSettingItem(
                        title = "Auto-start Recovery Breaks",
                        subtitle = "Immediately transition from focus to break",
                        checked = uiState.autoStartBreaks,
                        onCheckedChange = { viewModel.setAutoStartBreaks(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SwitchSettingItem(
                        title = "Auto-start Next Focus Block",
                        subtitle = "Begin next study sprint when break ends",
                        checked = uiState.autoStartPomodoros,
                        onCheckedChange = { viewModel.setAutoStartPomodoros(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SwitchSettingItem(
                        title = "Keep Screen On While Focused",
                        subtitle = "Prevents device display timeout during active timers",
                        checked = uiState.keepScreenAwake,
                        onCheckedChange = { viewModel.setKeepScreenAwake(it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SwitchSettingItem(
                        title = "Strict Mode Anti-Distraction",
                        subtitle = "Starts a 10s grace warning if you switch away from study",
                        checked = uiState.strictModeEnabled,
                        onCheckedChange = { viewModel.setStrictModeEnabled(it) }
                    )
                }
            }

            // Section: Completion Ringtones & Audio
            item {
                SettingsSectionCard(title = "Completion Sound & Ringtones", icon = Icons.Default.Notifications) {
                    Text(
                        text = "Select chime or ringtone played when a session completes:",
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Alarm sounds list
                    AlarmSound.values().forEach { sound ->
                        val isSelected = uiState.alarmSound == sound
                        val isCurrentlyPreviewing = uiState.isPreviewingAlarm && isSelected

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.surfaceVariant.copy(alpha = 0.4f))
                                .border(
                                    1.dp,
                                    if (isSelected) colors.primary else colors.outline.copy(alpha = 0.15f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    viewModel.setAlarmSound(sound)
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (sound == AlarmSound.CUSTOM_USER_AUDIO && uiState.customAudioTitle != null) {
                                            "Custom: ${uiState.customAudioTitle}"
                                        } else {
                                            sound.title
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) colors.primary else colors.onSurface
                                    )
                                    Text(
                                        text = sound.description,
                                        fontSize = 11.sp,
                                        color = colors.onSurfaceVariant
                                    )
                                }

                                if (!sound.isSilent) {
                                    IconButton(
                                        onClick = {
                                            if (isCurrentlyPreviewing) {
                                                viewModel.stopAlarmPreview()
                                            } else {
                                                viewModel.previewAlarmSound(sound)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentlyPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = "Preview",
                                            tint = if (isSelected) colors.primary else colors.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Button to import personal MP3 as ringtone
                    Button(
                        onClick = { ringtonePicker.launch("audio/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.secondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.hasCustomAudio) "Change Custom Audio File" else "Import Custom Ringtone (MP3)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Section: Appearance & Accent Colors
            item {
                SettingsSectionCard(title = "Appearance & Accent Colors", icon = Icons.Default.Palette) {
                    Text(
                        text = "Theme Mode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.values().forEach { mode ->
                            val isSelected = uiState.themeMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) colors.primary else colors.surfaceVariant)
                                    .clickable { viewModel.setThemeMode(mode) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val icon = when (mode) {
                                        ThemeMode.SYSTEM -> Icons.Default.BrightnessMedium
                                        ThemeMode.LIGHT -> Icons.Default.LightMode
                                        ThemeMode.DARK -> Icons.Default.DarkMode
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) colors.onPrimary else colors.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = mode.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) colors.onPrimary else colors.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Customizable Accent Palette",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AccentPalettes.forEachIndexed { index, palette ->
                            val isSelected = uiState.accentColorIndex == index
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { viewModel.setAccentColorIndex(index) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(palette.darkPrimary)
                                        .border(
                                            2.dp,
                                            if (isSelected) colors.onSurface else Color.Transparent,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = palette.name.substringBefore(" "),
                                    fontSize = 10.sp,
                                    color = if (isSelected) colors.primary else colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun IntervalSliderItem(
    title: String,
    value: String,
    currentVal: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Int,
    onValueChange: (Float) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
        }
        Slider(
            value = currentVal,
            onValueChange = onValueChange,
            valueRange = range,
            steps = ((range.endInclusive - range.start) / step).toInt() - 1,
            colors = SliderDefaults.colors(
                thumbColor = colors.primary,
                activeTrackColor = colors.primary
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}

@Composable
fun SwitchSettingItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = colors.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary
            )
        )
    }
}
