package app.spotlightguard.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DmRedirectTest {

    private val on = FeatureConfig(enabled = true, reopenAfterMinutes = 5)

    @Test
    fun disabledNeverRedirects() {
        assertFalse(DmRedirect.shouldRedirect(on.copy(enabled = false), Long.MAX_VALUE, 0))
    }

    @Test
    fun redirectsOnlyAfterEnoughAbsence() {
        assertFalse(DmRedirect.shouldRedirect(on, 4 * 60_000L, 12 * 60))
        assertTrue(DmRedirect.shouldRedirect(on, 5 * 60_000L, 12 * 60))
    }

    @Test
    fun zeroMeansEveryOpen() {
        assertTrue(DmRedirect.shouldRedirect(on.copy(reopenAfterMinutes = 0), 0L, 12 * 60))
    }

    @Test
    fun firstEverOpenRedirects() {
        assertTrue(DmRedirect.shouldRedirect(on, System.currentTimeMillis(), 12 * 60))
    }

    @Test
    fun timeWindowRestrictsWhenActive() {
        val c = on.copy(timeWindowEnabled = true, windowStartMinute = 9 * 60, windowEndMinute = 18 * 60)
        assertTrue(DmRedirect.shouldRedirect(c, Long.MAX_VALUE, 10 * 60))
        assertFalse(DmRedirect.shouldRedirect(c, Long.MAX_VALUE, 20 * 60))
    }
}
