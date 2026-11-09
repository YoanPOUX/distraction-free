package app.spotlightguard.service

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Snapchat. Plusieurs signaux, car l'arbre varie selon les versions :
 *  1. un élément « spotlight » de contenu (identifiant contenant « spotlight », hors icônes de
 *     navigation) visible et grand : la page Spotlight est affichée ;
 *  2. l'icône ou l'onglet « spotlight » de la barre de navigation, sélectionné (nœud, enfants,
 *     petits-enfants ou parents).
 * Le mode Diagnostic de l'app montre les identifiants vus pour recalibrer si besoin.
 */
object SpotlightDetector {

    private val NAV_HINTS = listOf("icon", "tab", "nav", "button", "label", "badge", "title")
    private val CAMERA_IDS = listOf("ngs_camera_icon", "camera_tab", "ngs_camera_button")
    private val CAMERA_LABELS = listOf("camera", "caméra", "appareil photo")

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

    /** Clique sur l'onglet Caméra. Renvoie false si on ne l'a pas trouvé. */
    fun goToCamera(root: AccessibilityNodeInfo): Boolean =
        NodeTools.clickViewId(root, CAMERA_IDS) || NodeTools.clickLabel(root, CAMERA_LABELS)
}
