package app.spotlightguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    private val now = 1_000_000L
    private val on = FeatureConfig(enabled = true)
    private val rules = on.copy(
        mode = Mode.RULES,
        dailyBudgetEnabled = false,
        sessionLimitEnabled = false,
        cooldownEnabled = false,
        timeWindowEnabled = false,
    )

    private fun eval(c: FeatureConfig, u: Usage = Usage(), minute: Int = 12 * 60) =
        RuleEngine.evaluate(c, u, now, minute)

    @Test
    fun disabledNeverBlocks() {
        assertFalse(eval(FeatureConfig(enabled = false)).block)
    }

    @Test
    fun totalModeAlwaysBlocks() {
        val d = eval(on.copy(mode = Mode.TOTAL))
        assertTrue(d.block)
        assertEquals(Reason.TOTAL, d.reason)
    }

    @Test
    fun rulesModeWithoutActiveRuleAllows() {
        assertFalse(eval(rules, Usage(dailyMs = 999_999_999L)).block)
    }

    @Test
    fun dailyBudgetBlocksAtLimit() {
        val c = rules.copy(dailyBudgetEnabled = true, dailyBudgetMinutes = 10)
        assertFalse(eval(c, Usage(dailyMs = 9 * 60_000L)).block)
        val d = eval(c, Usage(dailyMs = 10 * 60_000L))
        assertTrue(d.block)
        assertEquals(Reason.DAILY_BUDGET, d.reason)
    }

    @Test
    fun sessionLimitBlocksAtLimit() {
        val c = rules.copy(sessionLimitEnabled = true, sessionLimitMinutes = 5)
        assertFalse(eval(c, Usage(sessionMs = 4 * 60_000L)).block)
        assertEquals(Reason.SESSION_LIMIT, eval(c, Usage(sessionMs = 5 * 60_000L)).reason)
    }

    @Test
    fun cooldownBlocksUntilItEnds() {
        val c = rules.copy(cooldownEnabled = true)
        assertEquals(Reason.COOLDOWN, eval(c, Usage(cooldownUntil = now + 1)).reason)
        assertFalse(eval(c, Usage(cooldownUntil = now)).block)
        assertFalse(eval(rules, Usage(cooldownUntil = now + 1)).block)
    }

    @Test
    fun timeWindowCrossingMidnight() {
        val c = rules.copy(
            timeWindowEnabled = true,
            windowStartMinute = 22 * 60,
            windowEndMinute = 7 * 60,
        )
        assertTrue(eval(c, minute = 23 * 60).block)
        assertTrue(eval(c, minute = 3 * 60).block)
        assertFalse(eval(c, minute = 7 * 60).block)
        assertFalse(eval(c, minute = 12 * 60).block)
        assertTrue(eval(c, minute = 22 * 60).block)
    }

    @Test
    fun timeWindowSameDay() {
        assertTrue(RuleEngine.inWindow(13 * 60, 12 * 60, 14 * 60))
        assertFalse(RuleEngine.inWindow(14 * 60, 12 * 60, 14 * 60))
        assertFalse(RuleEngine.inWindow(5, 600, 600))
    }

    @Test
    fun graceOverridesEverything() {
        val c = on.copy(mode = Mode.TOTAL)
        assertFalse(eval(c, Usage(graceUntil = now + 1)).block)
        assertTrue(eval(c, Usage(graceUntil = now)).block)
    }

    @Test
    fun cooldownStartsOnlyAfterSessionLimit() {
        val c = rules.copy(cooldownEnabled = true, cooldownMinutes = 30)
        assertEquals(now + 30 * 60_000L, RuleEngine.cooldownEnd(c, Reason.SESSION_LIMIT, now))
        assertNull(RuleEngine.cooldownEnd(c, Reason.DAILY_BUDGET, now))
        assertNull(RuleEngine.cooldownEnd(rules, Reason.SESSION_LIMIT, now))
    }
}
