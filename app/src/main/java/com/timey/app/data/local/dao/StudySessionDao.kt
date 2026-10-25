package com.timey.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.timey.app.data.local.entity.StudySessionEntity
import kotlinx.coroutines.flow.Flow

data class SubjectDuration(
    val subject: String,
    val totalSeconds: Long,
    val sessionCount: Int
)

@Dao
interface StudySessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    @Query("SELECT * FROM study_sessions ORDER BY startTimeMillis DESC")
    fun getAllSessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions ORDER BY startTimeMillis DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<StudySessionEntity>>

    @Query("SELECT SUM(durationSeconds) FROM study_sessions WHERE isCompleted = 1")
    fun getTotalFocusSeconds(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM study_sessions WHERE isCompleted = 1")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT * FROM study_sessions WHERE startTimeMillis >= :startMillis AND startTimeMillis <= :endMillis ORDER BY startTimeMillis ASC")
    fun getSessionsBetween(startMillis: Long, endMillis: Long): Flow<List<StudySessionEntity>>

    @Query("SELECT subject, SUM(durationSeconds) as totalSeconds, COUNT(*) as sessionCount FROM study_sessions GROUP BY subject ORDER BY totalSeconds DESC")
    fun getSubjectDurations(): Flow<List<SubjectDuration>>

    @Query("DELETE FROM study_sessions")
    suspend fun clearAll()
}
