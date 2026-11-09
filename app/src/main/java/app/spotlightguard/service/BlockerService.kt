package app.spotlightguard.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import app.spotlightguard.core.Decision
import app.spotlightguard.core.DmRedirect
import app.spotlightguard.core.Feature
import app.spotlightguard.core.FeatureConfig
import app.spotlightguard.core.GuardSettings
import app.spotlightguard.core.RuleEngine
import app.spotlightguard.core.UsageTracker
import app.spotlightguard.data.DiagnosticStore
import app.spotlightguard.data.SettingsStore
import app.spotlightguard.data.UsageStore
import java.time.LocalTime

/**
 * Service d'accessibilité unique pour Snapchat et Instagram.
 *
 * - Snapchat : bloque Spotlight (fonction SPOTLIGHT).
 * - Instagram : bloque Reels en ouvrant les DM à la place (REELS), et ouvre les DM
 *   quand l'app se lance (DMS).
 *
 * Le service ne reçoit que les événements de ces deux apps. Une horloge d'une seconde
 * ne tourne que pendant que l'une d'elles est au premier plan avec une fonction active.
 */
class BlockerService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = Runnable { tick() }

    private lateinit var settings: SettingsStore
    private lateinit var usageStore: UsageStore
    private lateinit var overlay: BlockOverlay
    private val trackers = mutableMapOf<Feature, UsageTracker>()
    private var ready = false

    private var ticking = false
    private var lastPackage: String? = null
    private var lastBlockAt = 0L
    private var lastPersistAt = 0L
    private var lastDumpAt = 0L
    private var nullRoots = 0
    private var pendingDmUntil = 0L

    // DM au lancement d'Instagram
    private var igActive = false
    private var lastIgSeenAt = 0L
    private var dmAttempts = NO_DM_ATTEMPT

    override fun onServiceConnected() {
        super.onServiceConnected()
        settings = SettingsStore.get(this)
        usageStore = UsageStore.get(this)
        usageStore.refresh()
        overlay = BlockOverlay(this)
        trackers.clear()
        BLOCKING.forEach { feature -> trackers[feature] = UsageTracker(usageStore.usageOf(feature)) }
        ready = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!ready) return
        val pkg = event?.packageName?.toString() ?: return
        if (!hasEnabledFeature(pkg, settings.state.value)) return
        startTicking()
    }

    override fun onInterrupt() {
        stopTicking()
    }

    override fun onDestroy() {
        stopTicking()
        super.onDestroy()
    }

    private fun startTicking() {
        if (ticking) return
        ticking = true
        handler.post(ticker)
    }

    private fun stopTicking() {
        ticking = false
        handler.removeCallbacks(ticker)
        if (!ready) return
        overlay.hide()
        trackers.values.forEach { it.pause() }
        leaveInstagram()
        lastPackage = null
        persistAll(System.currentTimeMillis())
    }

    private fun hasEnabledFeature(pkg: String, s: GuardSettings): Boolean = when (pkg) {
        Targets.SNAPCHAT -> s.spotlight.enabled || s.stories.enabled
        Targets.INSTAGRAM -> s.reels.enabled || s.dms.enabled
        else -> false
    }

    private fun tick() {
        if (!ticking) return

        val s = settings.state.value
        val root = rootInActiveWindow
        val pkg = root?.packageName?.toString()
        if (root == null || pkg == null) {
            // Fenêtre en transition : on patiente quelques secondes avant d'arrêter.
            if (++nullRoots > MAX_NULL_ROOTS) stopTicking() else handler.postDelayed(ticker, TICK_MS)
            return
        }
        nullRoots = 0
        if (!hasEnabledFeature(pkg, s)) {
            stopTicking()
            return
        }

        val now = System.currentTimeMillis()
        if (pkg != lastPackage) {
            trackers.values.forEach { it.pause() }
            if (pkg != Targets.INSTAGRAM) leaveInstagram()
            lastPackage = pkg
        }

        val detection: String
        if (pkg == Targets.SNAPCHAT) {
            val onSpotlight = (s.spotlight.enabled || s.diagnosticEnabled) && SpotlightDetector.isSpotlight(root)
            val onStories = (s.stories.enabled || s.diagnosticEnabled) && SpotlightDetector.isStories(root)
            tickBlocking(Feature.SPOTLIGHT, s, onSpotlight, now)
            tickBlocking(Feature.STORIES, s, onStories, now)
            detection = "Snapchat · spotlight : ${yesNo(onSpotlight)} · amis/stories : ${yesNo(onStories)}"
        } else {
            tickDms(s.dms, root, now)
            if (pendingDmUntil > now && !(s.reels.enabled && InstagramDetector.isReels(root)) &&
                InstagramDetector.openDms(root)
            ) {
                pendingDmUntil = 0L
            }
            val onReels = (s.reels.enabled || s.diagnosticEnabled) && InstagramDetector.isReels(root)
            tickBlocking(Feature.REELS, s, onReels, now)
            detection = "Instagram · reels : ${yesNo(onReels)} · tentatives DM : $dmAttempts"
        }

        if (s.diagnosticEnabled && now - lastDumpAt >= DUMP_INTERVAL_MS) {
            lastDumpAt = now
            DiagnosticStore.set(NodeTools.dump(root, detection))
        }

        if (now - lastPersistAt >= PERSIST_INTERVAL_MS) persistAll(now)

        val fast = dmAttempts in 0 until MAX_DM_ATTEMPTS || pendingDmUntil > now
        handler.postDelayed(ticker, if (fast) FAST_TICK_MS else TICK_MS)
    }

    /** Suivi du temps et blocage pour Spotlight ou Reels. */
    private fun tickBlocking(feature: Feature, s: GuardSettings, onTarget: Boolean, now: Long) {
        val config = s.of(feature)
        if (!config.enabled) return
        val tracker = trackers.getValue(feature)

        val overlayShowing = overlay.isShowing
        // Pendant qu'un écran de blocage est affiché, on ne compte pas le temps.
        val usage = tracker.tick(now, onTarget && !overlayShowing, UsageStore.todayKey())

        if (overlayShowing && !onTarget && overlay.persistent) {
            overlay.hide()
        }

        if (onTarget && !overlayShowing && now - lastBlockAt > BLOCK_DEBOUNCE_MS) {
            val decision = RuleEngine.evaluate(config, usage, now, minuteOfDay())
            if (decision.block) block(feature, config, decision, now)
        }
    }

    /** Ouvre les DM une fois, juste après le lancement d'Instagram, avec quelques tentatives. */
    private fun tickDms(config: FeatureConfig, root: AccessibilityNodeInfo, now: Long) {
        if (!igActive) {
            igActive = true
            val awayMs = now - lastIgSeenAt
            dmAttempts = if (DmRedirect.shouldRedirect(config, awayMs, minuteOfDay())) 0 else NO_DM_ATTEMPT
        }
        lastIgSeenAt = now

        if (dmAttempts in 0 until MAX_DM_ATTEMPTS) {
            if (InstagramDetector.openDms(root)) dmAttempts = NO_DM_ATTEMPT else dmAttempts++
        }
    }

    private fun leaveInstagram() {
        igActive = false
        dmAttempts = NO_DM_ATTEMPT
    }

    private fun block(feature: Feature, config: FeatureConfig, decision: Decision, now: Long) {
        lastBlockAt = now
        val tracker = trackers.getValue(feature)

        RuleEngine.cooldownEnd(config, decision.reason, now)?.let { end ->
            tracker.usage = tracker.usage.copy(cooldownUntil = end)
        }

        // Aucun écran ni message : on quitte simplement la page bloquée.
        leave(feature)
        persistAll(now)
    }

    /**
     * Quitte la page bloquée : Spotlight et Amis/Stories -> onglet Chat ; Reels -> les DM (sinon l'accueil
     * d'Instagram). Dernier recours : écran d'accueil du téléphone.
     */
    private fun leave(feature: Feature) {
        val root: AccessibilityNodeInfo? = rootInActiveWindow
        val done = root != null && when (feature) {
            Feature.SPOTLIGHT, Feature.STORIES -> SpotlightDetector.openChat(root)
            Feature.REELS -> InstagramDetector.openDms(root)
            Feature.DMS -> false
        }
        if (done) return
        if (feature == Feature.REELS) {
            // Le lecteur Reels n'a pas d'icône DM : retour, puis on ouvre les DM dès qu'ils apparaissent.
            pendingDmUntil = System.currentTimeMillis() + PENDING_DM_MS
            performGlobalAction(GLOBAL_ACTION_BACK)
        } else {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    private fun persistAll(now: Long) {
        trackers.forEach { (feature, tracker) -> usageStore.save(feature, tracker.usage) }
        lastPersistAt = now
    }

    private fun minuteOfDay(): Int = LocalTime.now().let { it.hour * 60 + it.minute }

    private fun yesNo(value: Boolean) = if (value) "oui" else "non"

    private companion object {
        val BLOCKING = listOf(Feature.SPOTLIGHT, Feature.REELS, Feature.STORIES)

        const val TICK_MS = 1_000L
        const val BLOCK_DEBOUNCE_MS = 1_200L
        const val PERSIST_INTERVAL_MS = 5_000L
        const val DUMP_INTERVAL_MS = 4_000L
        const val STRICT_MESSAGE_MS = 2_500L
        const val NO_DM_ATTEMPT = -1
        const val MAX_DM_ATTEMPTS = 24
        const val FAST_TICK_MS = 500L
        const val MAX_NULL_ROOTS = 5
        const val PENDING_DM_MS = 6_000L
    }
}
