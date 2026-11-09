package app.spotlightguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardSettingsTest {

    @Test
    fun unlinkedEditTouchesOnlyOneFeature() {
        val n = GuardSettings().edit(Feature.REELS) { it.copy(dailyBudgetMinutes = 40) }
        assertEquals(40, n.reels.dailyBudgetMinutes)
        assertEquals(15, n.spotlight.dailyBudgetMinutes)
        assertEquals(15, n.dms.dailyBudgetMinutes)
    }

    @Test
    fun linkedEditCopiesRulesButKeepsEnabledFlags() {
        val s = GuardSettings(
            spotlight = FeatureConfig(enabled = true),
            reels = FeatureConfig(enabled = false),
            dms = FeatureConfig(enabled = true),
            linked = true,
        )
        val n = s.edit(Feature.REELS) { it.copy(dailyBudgetMinutes = 40) }
        assertEquals(40, n.spotlight.dailyBudgetMinutes)
        assertEquals(40, n.reels.dailyBudgetMinutes)
        assertEquals(40, n.dms.dailyBudgetMinutes)
        assertTrue(n.spotlight.enabled)
        assertFalse(n.reels.enabled)
        assertTrue(n.dms.enabled)
    }

    @Test
    fun withEnabledNeverTouchesOtherFeatures() {
        val s = GuardSettings(linked = true)
        val n = s.withEnabled(Feature.DMS, true)
        assertTrue(n.dms.enabled)
        assertFalse(n.spotlight.enabled)
        assertFalse(n.reels.enabled)
    }
}
