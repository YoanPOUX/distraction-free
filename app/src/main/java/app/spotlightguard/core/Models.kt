package app.spotlightguard.core

enum class Mode { TOTAL, RULES }

enum class Strictness { STRICT, SOFT }

enum class Reason { TOTAL, TIME_WINDOW, COOLDOWN, DAILY_BUDGET, SESSION_LIMIT }

/** Les trois fonctions de l'app, chacune avec ses propres réglages. */
enum class Feature {
    /** Snapchat : bloque l'onglet Spotlight. */
    SPOTLIGHT,

    /** Instagram : bloque l'onglet Reels et ouvre les DM à la place. */
    REELS,

    /** Instagram : ouvre directement les DM quand l'app se lance. */
    DMS,
}

/** Réglages d'une fonction. Les heures sont en minutes depuis minuit. */
data class FeatureConfig(
    val enabled: Boolean = false,
    val mode: Mode = Mode.TOTAL,
    val strictness: Strictness = Strictness.STRICT,

    val dailyBudgetEnabled: Boolean = true,
    val dailyBudgetMinutes: Int = 15,

    val sessionLimitEnabled: Boolean = false,
    val sessionLimitMinutes: Int = 5,

    val cooldownEnabled: Boolean = false,
    val cooldownMinutes: Int = 30,

    /** Blocage : bloqué pendant la plage. DM au lancement : actif seulement pendant la plage. */
    val timeWindowEnabled: Boolean = false,
    val windowStartMinute: Int = 22 * 60,
    val windowEndMinute: Int = 7 * 60,

    /** Durée accordée par le bouton « Continuer » (mode souple). */
    val graceMinutes: Int = 2,

    /** DM au lancement : n'ouvre les DM que si Instagram était fermé depuis au moins ce délai (0 = toujours). */
    val reopenAfterMinutes: Int = 5,
)

data class GuardSettings(
    val spotlight: FeatureConfig = FeatureConfig(),
    val reels: FeatureConfig = FeatureConfig(),
    val dms: FeatureConfig = FeatureConfig(),
    /** Réglages liés : une modification s'applique à toutes les fonctions (sauf l'activation). */
    val linked: Boolean = false,
    val diagnosticEnabled: Boolean = false,
) {
    fun of(feature: Feature): FeatureConfig = when (feature) {
        Feature.SPOTLIGHT -> spotlight
        Feature.REELS -> reels
        Feature.DMS -> dms
    }

    fun withConfig(feature: Feature, config: FeatureConfig): GuardSettings = when (feature) {
        Feature.SPOTLIGHT -> copy(spotlight = config)
        Feature.REELS -> copy(reels = config)
        Feature.DMS -> copy(dms = config)
    }

    fun withEnabled(feature: Feature, enabled: Boolean): GuardSettings =
        withConfig(feature, of(feature).copy(enabled = enabled))

    /**
     * Modifie les réglages d'une fonction. Si les réglages sont liés, le résultat est
     * recopié sur les autres fonctions, qui gardent chacune leur propre activation.
     */
    fun edit(feature: Feature, transform: (FeatureConfig) -> FeatureConfig): GuardSettings {
        val edited = transform(of(feature))
        if (!linked) return withConfig(feature, edited)
        return Feature.entries.fold(this) { acc, f ->
            if (f == feature) {
                acc.withConfig(f, edited)
            } else {
                acc.withConfig(f, edited.copy(enabled = acc.of(f).enabled))
            }
        }
    }
}

/** Temps passé et états temporaires d'une fonction. Les instants sont en millisecondes epoch. */
data class Usage(
    val dailyMs: Long = 0L,
    val sessionMs: Long = 0L,
    val cooldownUntil: Long = 0L,
    val graceUntil: Long = 0L,
)

data class Decision(val block: Boolean, val reason: Reason? = null) {
    companion object {
        val ALLOW = Decision(false)
    }
}
