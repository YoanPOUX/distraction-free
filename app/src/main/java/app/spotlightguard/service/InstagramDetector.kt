package app.spotlightguard.service

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Instagram : repère l'onglet Reels (sélectionné) et sait cliquer sur les DM ou l'accueil.
 * Les libellés viennent des descriptions d'accessibilité des icônes. S'ils changent,
 * le mode « Diagnostic » de l'app montre ce que le service voit.
 */
object InstagramDetector {

    private val REELS_LABELS = listOf("reels")
    private val DM_LABELS = listOf("messages", "direct", "messenger")
    private val HOME_LABELS = listOf("home", "accueil")

    fun isReels(root: AccessibilityNodeInfo): Boolean =
        NodeTools.isTabActive(root, REELS_LABELS)

    /** Ouvre la messagerie (icône ou onglet des DM). */
    fun openDms(root: AccessibilityNodeInfo): Boolean =
        NodeTools.clickLabel(root, DM_LABELS)

    fun openHome(root: AccessibilityNodeInfo): Boolean =
        NodeTools.clickLabel(root, HOME_LABELS)
}
