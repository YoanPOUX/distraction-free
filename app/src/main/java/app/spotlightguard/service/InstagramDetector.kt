package app.spotlightguard.service

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Instagram. La sélection des onglets n'est pas fiable dans l'arbre d'accessibilité (l'onglet actif
 * peut se déclarer non sélectionné), donc Reels est reconnu surtout par ses conteneurs de lecteur,
 * qui existent quelle que soit la façon d'y arriver. Les DM sont ouverts via leur identifiant de vue,
 * puis via leur description.
 */
object InstagramDetector {

    private val REELS_PLAYER_IDS = listOf(
        "clips_viewer_view_pager",
        "clips_video_container",
        "clips_media_component",
    )
    private val REELS_TAB_IDS = listOf("clips_tab")
    private val DM_IDS = listOf("action_bar_inbox_button", "direct_tab", "inbox_button", "action_bar_button_inbox")
    private val DM_LABELS = listOf("messages", "message", "direct", "messenger", "messagerie")
    private val HOME_IDS = listOf("feed_tab")
    private val HOME_LABELS = listOf("home", "accueil")

    fun isReels(root: AccessibilityNodeInfo): Boolean {
        for (node in NodeTools.all(root)) {
            val id = NodeTools.viewId(node)
            if (id.isEmpty()) continue
            if (id in REELS_PLAYER_IDS && NodeTools.isBig(node, root, 0.5f)) return true
            if (id in REELS_TAB_IDS && NodeTools.isSelectedDeep(node)) return true
        }
        return false
    }

    /** Ouvre la messagerie (icône en haut à droite de l'accueil, ou onglet DM). */
    fun openDms(root: AccessibilityNodeInfo): Boolean =
        NodeTools.clickViewId(root, DM_IDS) || NodeTools.clickLabel(root, DM_LABELS)

    fun openHome(root: AccessibilityNodeInfo): Boolean =
        NodeTools.clickViewId(root, HOME_IDS) || NodeTools.clickLabel(root, HOME_LABELS)
}
