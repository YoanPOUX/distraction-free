package app.spotlightguard.core

/**
 * Cumule le temps passé sur Spotlight (jour + session).
 * Appelé une fois par seconde par le service tant que Snapchat est au premier plan.
 *
 * Une session se termine quand on reste plus de [sessionGapMs] hors de Spotlight.
 */
class UsageTracker(
    initial: Usage = Usage(),
    private val sessionGapMs: Long = SESSION_GAP_MS,
) {
    var usage: Usage = initial

    private var dayKey: String? = null
    private var lastTickAt = 0L
    private var lastSpotlightAt = 0L

    /** À appeler quand le suivi s'interrompt, pour ne pas compter le temps de la pause. */
    fun pause() {
        lastTickAt = 0L
    }

    fun tick(nowMs: Long, onSpotlight: Boolean, today: String): Usage {
        if (dayKey != null && dayKey != today) {
            usage = usage.copy(dailyMs = 0L)
        }
        dayKey = today

        if (onSpotlight) {
            val delta =
                if (lastTickAt == 0L) 0L else (nowMs - lastTickAt).coerceIn(0L, MAX_DELTA_MS)
            if (lastSpotlightAt == 0L || nowMs - lastSpotlightAt > sessionGapMs) {
                usage = usage.copy(sessionMs = 0L)
            }
            usage = usage.copy(
                dailyMs = usage.dailyMs + delta,
                sessionMs = usage.sessionMs + delta,
            )
            lastSpotlightAt = nowMs
        }

        lastTickAt = nowMs
        return usage
    }

    companion object {
        const val SESSION_GAP_MS = 2 * 60_000L
        private const val MAX_DELTA_MS = 3_000L
    }
}
