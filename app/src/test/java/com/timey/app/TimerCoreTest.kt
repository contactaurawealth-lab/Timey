package com.timey.app

import com.timey.app.core.blocker.BlockerManager
import com.timey.app.domain.model.PomodoroPhase
import com.timey.app.domain.model.TimerMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TimerCoreTest {

    @Before
    fun setUp() {
        BlockerManager.updateConfig(
            blockingEnabled = true,
            packages = setOf("com.instagram.android", "com.zhiliaoapp.musically"),
            pinEnabled = true,
            pin = "4321"
        )
    }

    @Test
    fun testTimerModesDefaults() {
        assertEquals(25, TimerMode.POMODORO.defaultMinutes)
        assertEquals(135, TimerMode.DEEP_WORK_135.defaultMinutes)
        assertEquals(50, TimerMode.ULTRADIAN.defaultMinutes)
        assertEquals(60, TimerMode.EXAM.defaultMinutes)
        assertEquals(0, TimerMode.FLOWMODORO.defaultMinutes)
    }

    @Test
    fun testPomodoroPhases() {
        assertEquals("Focus Block", PomodoroPhase.FOCUS.label)
        assertFalse(PomodoroPhase.FOCUS.isBreak)

        assertEquals("Short Rest", PomodoroPhase.SHORT_BREAK.label)
        assertTrue(PomodoroPhase.SHORT_BREAK.isBreak)

        assertEquals("Deep Recovery", PomodoroPhase.LONG_BREAK.label)
        assertTrue(PomodoroPhase.LONG_BREAK.isBreak)
    }

    @Test
    fun testBlockerManagerLogic() {
        // When session is NOT active, should not block
        BlockerManager.updateSessionState(isActive = false, subject = "Math", remainingSec = 1500)
        assertFalse(BlockerManager.shouldBlock("com.instagram.android"))

        // When session IS active and app is blocked
        BlockerManager.updateSessionState(isActive = true, subject = "Math", remainingSec = 1500)
        assertTrue(BlockerManager.shouldBlock("com.instagram.android"))
        assertTrue(BlockerManager.shouldBlock("com.zhiliaoapp.musically"))

        // Non-blocked apps should not be blocked
        assertFalse(BlockerManager.shouldBlock("com.google.android.calculator"))

        // System whitelisted apps should never be blocked
        assertFalse(BlockerManager.shouldBlock("com.timey.app"))
        assertFalse(BlockerManager.shouldBlock("com.android.systemui"))
        assertFalse(BlockerManager.shouldBlock("com.android.dialer"))

        // PIN Verification
        assertTrue(BlockerManager.verifyPin("4321"))
        assertFalse(BlockerManager.verifyPin("0000"))
    }
}
