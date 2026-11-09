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
    private val CHAT_IDS = listOf("ngs_chat_icon", "chat_tab", "ngs_chat_button")
    private val CHAT_LABELS = listOf("chat", "chats", "discuter", "discussions")
    private val STORIES_LABELS = listOf("stories", "story", "friends", "amis")
    private val STORIES_ID_PARTS = listOf("friend", "stories", "story")

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

    /** Onglet Amis / Stories de la barre de navigation, sélectionné. */
    fun isStories(root: AccessibilityNodeInfo): Boolean {
        for (node in NodeTools.all(root)) {
            val id = NodeTools.viewId(node)
            val navIcon = id.startsWith("ngs_") && STORIES_ID_PARTS.any { id.contains(it) }
            val labelled = NodeTools.labels(node).any { label -> STORIES_LABELS.any { NodeTools.matches(label, it) } }
            if ((navIcon || labelled) && NodeTools.isSelectedDeep(node)) return true
        }
        return false
    }

    /** Clique sur l'onglet Chat. Renvoie false si on ne l'a pas trouvé. */
    fun openChat(root: AccessibilityNodeInfo): Boolean =
        NodeTools.clickViewId(root, CHAT_IDS) || NodeTools.clickLabel(root, CHAT_LABELS)
}
