package com.timey.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationSeconds: Int,
    val subject: String,
    val topic: String,
    val mode: String,
    val isCompleted: Boolean,
    val distractionsCount: Int = 0,
    val xpEarned: Int = 0
)
