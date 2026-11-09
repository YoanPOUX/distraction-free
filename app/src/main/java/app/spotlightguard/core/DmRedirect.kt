package app.spotlightguard.core

/** Décide s'il faut ouvrir les DM d'Instagram au lancement de l'app. Fonction pure. */
object DmRedirect {

    private const val MINUTE_MS = 60_000L

    /**
     * @param awayMs temps écoulé depuis la dernière fois qu'Instagram était au premier plan.
     */
    fun shouldRedirect(config: FeatureConfig, awayMs: Long, minuteOfDay: Int): Boolean {
        if (!config.enabled) return false
        if (config.timeWindowEnabled &&
            !RuleEngine.inWindow(minuteOfDay, config.windowStartMinute, config.windowEndMinute)
        ) {
            return false
        }
        return awayMs >= config.reopenAfterMinutes * MINUTE_MS
    }
}
