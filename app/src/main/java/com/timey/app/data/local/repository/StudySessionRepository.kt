package com.timey.app.data.local.repository

import com.timey.app.data.local.dao.StudySessionDao
import com.timey.app.data.local.dao.SubjectDuration
import com.timey.app.data.local.entity.StudySessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

data class StreakInfo(
    val currentStreak: Int,
    val bestStreak: Int,
    val studiedToday: Boolean
)

data class DailyStudyMinutes(
    val dayOfWeek: String, // "Mon", "Tue", etc.
    val dayOfMonth: Int,
    val minutes: Int,
    val isToday: Boolean
)

class StudySessionRepository(
    private val dao: StudySessionDao
) {
    val allSessions: Flow<List<StudySessionEntity>> = dao.getAllSessions()

    val totalFocusSeconds: Flow<Long> = dao.getTotalFocusSeconds().map { it ?: 0L }

    val completedPomodorosCount: Flow<Int> = dao.getCompletedCount()

    val subjectDurations: Flow<List<SubjectDuration>> = dao.getSubjectDurations()

    fun getRecentSessions(limit: Int = 10): Flow<List<StudySessionEntity>> =
        dao.getRecentSessions(limit)

    suspend fun saveSession(session: StudySessionEntity): Long {
        return dao.insertSession(session)
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }

    /**
     * Calculates streak information by inspecting calendar days with at least one completed study session.
     */
    fun getStreakInfo(): Flow<StreakInfo> {
        return dao.getAllSessions().map { sessions ->
            val completedDates = sessions
                .filter { it.isCompleted && it.durationSeconds >= 60 }
                .map { getDayEpoch(it.startTimeMillis) }
                .distinct()
                .sortedDescending()

            if (completedDates.isEmpty()) {
                return@map StreakInfo(currentStreak = 0, bestStreak = 0, studiedToday = false)
            }

            val todayEpoch = getDayEpoch(System.currentTimeMillis())
            val studiedToday = completedDates.contains(todayEpoch)

            var currentStreak = 0
            var expectedDay = if (studiedToday) todayEpoch else todayEpoch - 1

            for (day in completedDates) {
                if (day == expectedDay) {
                    currentStreak++
                    expectedDay--
                } else if (day < expectedDay) {
                    break
                }
            }

            // Calculate historical best streak
            val sortedAscending = completedDates.sorted()
            var bestStreak = 0
            var runningStreak = 0
            var prevDay = -1L

            for (day in sortedAscending) {
                if (prevDay == -1L || day == prevDay + 1) {
                    runningStreak++
                } else {
                    runningStreak = 1
                }
                prevDay = day
                if (runningStreak > bestStreak) {
                    bestStreak = runningStreak
                }
            }

            StreakInfo(
                currentStreak = currentStreak,
                bestStreak = maxOf(bestStreak, currentStreak),
                studiedToday = studiedToday
            )
        }
    }

    /**
     * Returns 7 days of study minutes for the current week (e.g. Mon - Sun).
     */
    fun getWeeklyStudyData(): Flow<List<DailyStudyMinutes>> {
        val calendar = Calendar.getInstance()
        // Find Monday of current week
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val mondayStart = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val sundayEnd = calendar.timeInMillis - 1

        val todayCal = Calendar.getInstance()
        val todayDayOfYear = todayCal.get(Calendar.DAY_OF_YEAR)
        val todayYear = todayCal.get(Calendar.YEAR)

        return dao.getSessionsBetween(mondayStart, sundayEnd).map { sessions ->
            val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val result = mutableListOf<DailyStudyMinutes>()

            val iterCal = Calendar.getInstance()
            iterCal.timeInMillis = mondayStart

            for (i in 0 until 7) {
                val dayStart = iterCal.timeInMillis
                val dayOfMonth = iterCal.get(Calendar.DAY_OF_MONTH)
                val isToday = (iterCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear &&
                        iterCal.get(Calendar.YEAR) == todayYear)

                iterCal.add(Calendar.DAY_OF_YEAR, 1)
                val dayEnd = iterCal.timeInMillis - 1

                val totalSecForDay = sessions
                    .filter { it.startTimeMillis in dayStart..dayEnd }
                    .sumOf { it.durationSeconds }

                result.add(
                    DailyStudyMinutes(
                        dayOfWeek = dayNames[i],
                        dayOfMonth = dayOfMonth,
                        minutes = totalSecForDay / 60,
                        isToday = isToday
                    )
                )
            }
            result
        }
    }

    private fun getDayEpoch(timeMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis / (24 * 60 * 60 * 1000L)
    }
}
