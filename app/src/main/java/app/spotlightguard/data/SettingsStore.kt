package app.spotlightguard.data

import android.content.Context
import android.content.SharedPreferences
import app.spotlightguard.core.Feature
import app.spotlightguard.core.FeatureConfig
import app.spotlightguard.core.GuardSettings
import app.spotlightguard.core.Mode
import app.spotlightguard.core.Strictness
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Réglages persistés, partagés entre l'écran et le service (même processus). */
class SettingsStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<GuardSettings> = _state.asStateFlow()

    @Synchronized
    fun update(transform: (GuardSettings) -> GuardSettings) {
        val next = transform(_state.value)
        write(next)
        _state.value = next
    }

    fun edit(feature: Feature, transform: (FeatureConfig) -> FeatureConfig) =
        update { it.edit(feature, transform) }

    fun setEnabled(feature: Feature, enabled: Boolean) =
        update { it.withEnabled(feature, enabled) }

    private fun read(): GuardSettings = GuardSettings(
        spotlight = readFeature(Feature.SPOTLIGHT),
        reels = readFeature(Feature.REELS),
        dms = readFeature(Feature.DMS),
        stories = readFeature(Feature.STORIES),
        chats = readFeature(Feature.CHATS),
        linked = prefs.getBoolean("linked", false),
        diagnosticEnabled = prefs.getBoolean("diagnosticEnabled", false),
    )

    private fun write(s: GuardSettings) {
        val editor = prefs.edit()
            .putBoolean("linked", s.linked)
            .putBoolean("diagnosticEnabled", s.diagnosticEnabled)
        Feature.entries.forEach { writeFeature(editor, it, s.of(it)) }
        editor.apply()
    }

    private fun readFeature(feature: Feature): FeatureConfig {
        val p = prefix(feature)
        val d = FeatureConfig()
        return FeatureConfig(
            enabled = prefs.getBoolean(p + "enabled", d.enabled),
            mode = enumOf(prefs.getString(p + "mode", null), d.mode),
            strictness = enumOf(prefs.getString(p + "strictness", null), d.strictness),
            dailyBudgetEnabled = prefs.getBoolean(p + "dailyBudgetEnabled", d.dailyBudgetEnabled),
            dailyBudgetMinutes = prefs.getInt(p + "dailyBudgetMinutes", d.dailyBudgetMinutes),
            sessionLimitEnabled = prefs.getBoolean(p + "sessionLimitEnabled", d.sessionLimitEnabled),
            sessionLimitMinutes = prefs.getInt(p + "sessionLimitMinutes", d.sessionLimitMinutes),
            cooldownEnabled = prefs.getBoolean(p + "cooldownEnabled", d.cooldownEnabled),
            cooldownMinutes = prefs.getInt(p + "cooldownMinutes", d.cooldownMinutes),
            timeWindowEnabled = prefs.getBoolean(p + "timeWindowEnabled", d.timeWindowEnabled),
            windowStartMinute = prefs.getInt(p + "windowStartMinute", d.windowStartMinute),
            windowEndMinute = prefs.getInt(p + "windowEndMinute", d.windowEndMinute),
            graceMinutes = prefs.getInt(p + "graceMinutes", d.graceMinutes),
            reopenAfterMinutes = prefs.getInt(p + "reopenAfterMinutes", d.reopenAfterMinutes),
        )
    }

    private fun writeFeature(editor: SharedPreferences.Editor, feature: Feature, c: FeatureConfig) {
        val p = prefix(feature)
        editor
            .putBoolean(p + "enabled", c.enabled)
            .putString(p + "mode", c.mode.name)
            .putString(p + "strictness", c.strictness.name)
            .putBoolean(p + "dailyBudgetEnabled", c.dailyBudgetEnabled)
            .putInt(p + "dailyBudgetMinutes", c.dailyBudgetMinutes)
            .putBoolean(p + "sessionLimitEnabled", c.sessionLimitEnabled)
            .putInt(p + "sessionLimitMinutes", c.sessionLimitMinutes)
            .putBoolean(p + "cooldownEnabled", c.cooldownEnabled)
            .putInt(p + "cooldownMinutes", c.cooldownMinutes)
            .putBoolean(p + "timeWindowEnabled", c.timeWindowEnabled)
            .putInt(p + "windowStartMinute", c.windowStartMinute)
            .putInt(p + "windowEndMinute", c.windowEndMinute)
            .putInt(p + "graceMinutes", c.graceMinutes)
            .putInt(p + "reopenAfterMinutes", c.reopenAfterMinutes)
    }

    private fun prefix(feature: Feature) = feature.name.lowercase() + "_"

    private inline fun <reified T : Enum<T>> enumOf(name: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: fallback

    companion object {
        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context).also { instance = it }
            }
    }
}
