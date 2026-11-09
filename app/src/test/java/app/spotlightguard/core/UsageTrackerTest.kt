package app.spotlightguard.core

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageTrackerTest {

    @Test
    fun accumulatesWhileOnSpotlight() {
        val t = UsageTracker()
        t.tick(1_000, true, "d1")
        t.tick(2_000, true, "d1")
        val u = t.tick(3_000, true, "d1")
        assertEquals(2_000L, u.dailyMs)
        assertEquals(2_000L, u.sessionMs)
    }

    @Test
    fun doesNotCountOutsideSpotlight() {
        val t = UsageTracker()
        t.tick(1_000, true, "d1")
        t.tick(2_000, true, "d1")
        val u = t.tick(3_000, false, "d1")
        assertEquals(1_000L, u.dailyMs)
    }

    @Test
    fun sessionResetsAfterGapButDailyKeeps() {
        val t = UsageTracker(sessionGapMs = 120_000L)
        t.tick(1_000, true, "d1")
        t.tick(2_000, true, "d1")
        t.pause()
        val u = t.tick(300_000, true, "d1")
        assertEquals(1_000L, u.dailyMs)
        assertEquals(0L, u.sessionMs)
    }

    @Test
    fun sessionSurvivesShortAbsence() {
        val t = UsageTracker(sessionGapMs = 120_000L)
        t.tick(1_000, true, "d1")
        t.tick(2_000, true, "d1")
        t.pause()
        val u = t.tick(30_000, true, "d1")
        assertEquals(1_000L, u.sessionMs)
    }

    @Test
    fun dailyResetsOnNewDay() {
        val t = UsageTracker()
        t.tick(1_000, true, "d1")
        t.tick(2_000, true, "d1")
        t.tick(3_000, true, "d1")
        val u = t.tick(4_000, true, "d2")
        assertEquals(1_000L, u.dailyMs)
    }
}
