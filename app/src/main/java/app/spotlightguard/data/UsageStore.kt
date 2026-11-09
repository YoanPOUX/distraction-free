package app.spotlightguard.data

import android.content.Context
import android.content.SharedPreferences
import app.spotlightguard.core.Feature
import app.spotlightguard.core.Usage
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Usage du jour persisté par fonction (le temps cumulé repart à zéro chaque jour). */
class UsageStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("usage", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(loadAll())
    val state: StateFlow<Map<Feature, Usage>> = _state.asStateFlow()

    fun usageOf(feature: Feature): Usage = _state.value[feature] ?: Usage()

    /** Recharge depuis le disque (utile au retour sur l'écran, ou après minuit). */
    fun refresh() {
        _state.value = loadAll()
    }

    fun save(feature: Feature, usage: Usage) {
        val p = prefix(feature)
        prefs.edit()
            .putString(p + "day", todayKey())
            .putLong(p + "daily", usage.dailyMs)
            .putLong(p + "cooldown", usage.cooldownUntil)
            .putLong(p + "grace", usage.graceUntil)
            .apply()
        _state.value = _state.value + (feature to usage)
    }

    private fun loadAll(): Map<Feature, Usage> = Feature.entries.associateWith { load(it) }

    private fun load(feature: Feature): Usage {
        val p = prefix(feature)
        val sameDay = prefs.getString(p + "day", null) == todayKey()
        return Usage(
            dailyMs = if (sameDay) prefs.getLong(p + "daily", 0L) else 0L,
            cooldownUntil = prefs.getLong(p + "cooldown", 0L),
            graceUntil = prefs.getLong(p + "grace", 0L),
        )
    }

    private fun prefix(feature: Feature) = feature.name.lowercase() + "_"

    companion object {
        @Volatile
        private var instance: UsageStore? = null

        fun get(context: Context): UsageStore =
            instance ?: synchronized(this) {
                instance ?: UsageStore(context).also { instance = it }
            }

        fun todayKey(): String = LocalDate.now().toString()
    }
}
