package com.timey.app.features.analytics.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timey.app.data.local.dao.SubjectDuration
import com.timey.app.data.local.entity.StudySessionEntity
import com.timey.app.data.local.repository.DailyStudyMinutes
import com.timey.app.data.local.repository.StreakInfo
import com.timey.app.features.timer.viewmodel.TimerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val totalFocusSeconds by viewModel.studySessionRepository.totalFocusSeconds.collectAsState(initial = 0L)
    val completedCount by viewModel.studySessionRepository.completedPomodorosCount.collectAsState(initial = 0)
    val streakInfo by viewModel.studySessionRepository.getStreakInfo().collectAsState(
        initial = StreakInfo(currentStreak = 0, bestStreak = 0, studiedToday = false)
    )
    val weeklyData by viewModel.studySessionRepository.getWeeklyStudyData().collectAsState(initial = emptyList())
    val subjectDurations by viewModel.studySessionRepository.subjectDurations.collectAsState(initial = emptyList())
    val allSessions by viewModel.studySessionRepository.allSessions.collectAsState(initial = emptyList())

    val totalHours = totalFocusSeconds / 3600
    val totalMinutes = (totalFocusSeconds % 3600) / 60
    val totalFocusFormatted = if (totalHours > 0) "${totalHours}h ${totalMinutes}m" else "${totalMinutes}m"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Study Insights & Analytics",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )
        Text(
            text = "Track your cognitive consistency, subjects, and streaks",
            fontSize = 12.sp,
            color = colors.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 2x2 Metric Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Total Focus",
                        value = totalFocusFormatted,
                        subtitle = "${allSessions.size} total sessions",
                        icon = Icons.Default.HourglassBottom,
                        iconColor = colors.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Streak Flame",
                        value = "${streakInfo.currentStreak} Days",
                        subtitle = "Best: ${streakInfo.bestStreak} days",
                        icon = Icons.Default.LocalFireDepartment,
                        iconColor = colors.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Completed Cycles",
                        value = "$completedCount",
                        subtitle = "Finished study intervals",
                        icon = Icons.Default.CheckCircle,
                        iconColor = colors.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Weekly Total",
                        value = "${weeklyData.sumOf { it.minutes }}m",
                        subtitle = "Last 7 days",
                        icon = Icons.Default.DateRange,
                        iconColor = colors.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Weekly Study Bar Chart
            item {
                WeeklyStudyBarChartCard(weeklyData = weeklyData)
            }

            // Subject Distribution Breakdown
            item {
                SubjectBreakdownCard(
                    subjectDurations = subjectDurations,
                    totalSeconds = totalFocusSeconds
                )
            }

            // Session History Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Session History (${allSessions.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                }
            }

            if (allSessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No study sessions recorded yet. Start your first focus block!",
                            fontSize = 13.sp,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(allSessions.take(20)) { session ->
                    SessionHistoryItem(session = session)
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WeeklyStudyBarChartCard(weeklyData: List<DailyStudyMinutes>) {
    val colors = MaterialTheme.colorScheme
    val maxMinutes = (weeklyData.maxOfOrNull { it.minutes } ?: 60).coerceAtLeast(60)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Focus Consistency",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = "Peak: ${maxMinutes}m",
                    fontSize = 11.sp,
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Bar Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (weeklyData.isEmpty()) return@Canvas

                    val barCount = weeklyData.size
                    val spacing = 16.dp.toPx()
                    val totalSpacing = spacing * (barCount - 1)
                    val barWidth = (size.width - totalSpacing) / barCount
                    val maxHeightPx = size.height - 30.dp.toPx()

                    weeklyData.forEachIndexed { index, day ->
                        val left = index * (barWidth + spacing)
                        val fraction = (day.minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f)
                        val barHeight = (fraction * maxHeightPx).coerceAtLeast(6.dp.toPx())
                        val top = maxHeightPx - barHeight

                        // Background pillar
                        drawRoundRect(
                            color = colors.surfaceVariant.copy(alpha = 0.5f),
                            topLeft = Offset(left, 0f),
                            size = Size(barWidth, maxHeightPx),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // Active Bar
                        val barColor = if (day.isToday) colors.secondary else colors.primary
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Day Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weeklyData.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(36.dp)
                    ) {
                        Text(
                            text = day.dayOfWeek,
                            fontSize = 11.sp,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (day.isToday) colors.secondary else colors.onSurfaceVariant
                        )
                        Text(
                            text = "${day.minutes}m",
                            fontSize = 9.sp,
                            color = colors.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectBreakdownCard(
    subjectDurations: List<SubjectDuration>,
    totalSeconds: Long
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subject-Wise Time",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = "${subjectDurations.size} Subjects",
                    fontSize = 11.sp,
                    color = colors.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (subjectDurations.isEmpty()) {
                Text(
                    text = "No subjects logged yet",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant
                )
            } else {
                subjectDurations.forEach { sub ->
                    val min = sub.totalSeconds / 60
                    val fraction = if (totalSeconds > 0) {
                        (sub.totalSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    val percent = (fraction * 100).toInt()

                    Column(modifier = Modifier.padding(vertical = 5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = sub.subject,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface
                            )
                            Text(
                                text = "${min}m ($percent%)",
                                fontSize = 12.sp,
                                color = colors.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = colors.primary,
                            trackColor = colors.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SessionHistoryItem(session: StudySessionEntity) {
    val colors = MaterialTheme.colorScheme
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val formattedDate = dateFormat.format(Date(session.startTimeMillis))
    val minutes = session.durationSeconds / 60

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
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
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (session.isCompleted) colors.primary.copy(alpha = 0.15f) else colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (session.isCompleted) Icons.Default.CheckCircle else Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (session.isCompleted) colors.primary else colors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = session.subject,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = session.mode,
                                fontSize = 9.sp,
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text(
                        text = session.topic.ifEmpty { "General Study" },
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${minutes}m",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = formattedDate,
                    fontSize = 10.sp,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}
