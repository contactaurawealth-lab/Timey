package com.timey.app

import com.timey.app.data.local.entity.StudySessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class StudyAnalyticsTest {

    @Test
    fun testStudySessionEntityCreation() {
        val now = System.currentTimeMillis()
        val session = StudySessionEntity(
            id = 1L,
            startTimeMillis = now - 1500_000L,
            endTimeMillis = now,
            durationSeconds = 1500,
            subject = "Mathematics",
            topic = "Calculus",
            mode = "Pomodoro",
            isCompleted = true,
            distractionsCount = 0,
            xpEarned = 250
        )

        assertEquals(1L, session.id)
        assertEquals(1500, session.durationSeconds)
        assertEquals("Mathematics", session.subject)
        assertTrue(session.isCompleted)
        assertEquals(250, session.xpEarned)
    }

    @Test
    fun testDeepWork135CycleCalculation() {
        val focusDurationSec = 135 * 60
        val breakDurationSec = 15 * 60

        assertEquals(8100, focusDurationSec)
        assertEquals(900, breakDurationSec)
        assertEquals(9, focusDurationSec / breakDurationSec) // 9:1 focus-to-break ratio
    }
}
