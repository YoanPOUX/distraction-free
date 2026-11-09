package app.spotlightguard.service

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Snapchat : pages Spotlight et Amis/Stories, et retour vers l'onglet Chat.
 * Signaux : un élément de contenu « spotlight » visible et grand, ou l'icône / l'onglet de la barre
 * de navigation sélectionné (nœud, enfants, petits-enfants ou parents).
 * Le mode Diagnostic de l'app montre les identifiants vus pour recalibrer si besoin.
 */
object SpotlightDetector {

    private val NAV_HINTS = listOf("icon", "tab", "nav", "button", "label", "badge", "title")
    private val CHAT_IDS = listOf("ngs_chat_icon_container", "ngs_chat_icon", "chat_tab")
    private val CHAT_LABELS = listOf("chat", "chats", "discuter", "discussions")

    fun isSpotlight(root: AccessibilityNodeInfo): Boolean {
        for (node in NodeTools.all(root)) {
            val id = NodeTools.viewId(node)
            val isNavLike = id.isNotEmpty() && NAV_HINTS.any { id.contains(it) }

            if (id.contains("spotlight") && !isNavLike && NodeTools.isBig(node, root, 0.4f)) return true

            val labelled = NodeTools.labels(node).any { it.contains("spotlight") }
            val navIcon = id.contains("spotlight") && isNavLike
            if ((labelled || navIcon) && NodeTools.isSelectedDeep(node)) return true
        }
        return false
    }

    /**
     * Page Stories (onglet « Stories » de la barre du bas, ex-Amis). Mesuré : les cartes portent
     * l'identifiant df_large_story, et l'en-tête affiche le texte « Stories » en haut de l'écran.
     * La barre de navigation ne déclare jamais l'onglet actif comme sélectionné.
     */
    fun isStories(root: AccessibilityNodeInfo): Boolean {
        val screenH = android.graphics.Rect().also { root.getBoundsInScreen(it) }.height()
        val r = android.graphics.Rect()
        for (node in NodeTools.all(root)) {
            val id = NodeTools.viewId(node)
            if (id.contains("large_story") && node.isVisibleToUser) return true
            if (node.text?.toString()?.trim() == "Stories") {
                node.getBoundsInScreen(r)
                if (node.isVisibleToUser && screenH > 0 && r.top < screenH * 0.25f) return true
            }
        }
        return false
    }

    /** Clique sur l'onglet Chat. Renvoie false si on ne l'a pas trouvé. */
    fun openChat(root: AccessibilityNodeInfo): Boolean =
        NodeTools.clickViewId(root, CHAT_IDS) || NodeTools.clickLabel(root, CHAT_LABELS)
}
