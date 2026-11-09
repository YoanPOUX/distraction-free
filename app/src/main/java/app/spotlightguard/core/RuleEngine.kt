package app.spotlightguard.core

/**
 * Fonction pure : réglages + usage + heure -> faut-il bloquer ?
 * Aucune dépendance Android, donc testable en JVM.
 */
object RuleEngine {

    private const val MINUTE_MS = 60_000L

    fun evaluate(
        config: FeatureConfig,
        usage: Usage,
        nowMs: Long,
        minuteOfDay: Int,
    ): Decision {
        if (!config.enabled) return Decision.ALLOW
        if (nowMs < usage.graceUntil) return Decision.ALLOW
        if (config.mode == Mode.TOTAL) return Decision(true, Reason.TOTAL)

        if (config.timeWindowEnabled &&
            inWindow(minuteOfDay, config.windowStartMinute, config.windowEndMinute)
        ) {
            return Decision(true, Reason.TIME_WINDOW)
        }

        if (config.cooldownEnabled && nowMs < usage.cooldownUntil) {
            return Decision(true, Reason.COOLDOWN)
        }

        if (config.dailyBudgetEnabled &&
            usage.dailyMs >= config.dailyBudgetMinutes * MINUTE_MS
        ) {
            return Decision(true, Reason.DAILY_BUDGET)
        }

        if (config.sessionLimitEnabled &&
            usage.sessionMs >= config.sessionLimitMinutes * MINUTE_MS
        ) {
            return Decision(true, Reason.SESSION_LIMIT)
        }

        return Decision.ALLOW
    }

    /** Instant de fin de pause à enregistrer quand une limite de session vient de bloquer. */
    fun cooldownEnd(config: FeatureConfig, reason: Reason?, nowMs: Long): Long? =
        if (reason == Reason.SESSION_LIMIT &&
            config.mode == Mode.RULES &&
            config.cooldownEnabled
        ) {
            nowMs + config.cooldownMinutes * MINUTE_MS
        } else {
            null
        }

    /** Plage [start, end[ en minutes depuis minuit, qui peut passer minuit (ex. 22:00 -> 07:00). */
    fun inWindow(minuteOfDay: Int, start: Int, end: Int): Boolean = when {
        start == end -> false
        start < end -> minuteOfDay in start until end
        else -> minuteOfDay >= start || minuteOfDay < end
    }
}
