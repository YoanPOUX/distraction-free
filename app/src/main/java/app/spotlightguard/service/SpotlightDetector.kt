package app.spotlightguard.service

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Repère l'onglet Spotlight de Snapchat : on cherche un onglet « Spotlight » sélectionné.
 * Si une mise à jour de Snapchat casse la détection, active « Diagnostic » dans l'app pour voir
 * l'arbre de vues et ajuster les libellés ci-dessous.
 */
object SpotlightDetector {

    private val SPOTLIGHT_LABELS = listOf("spotlight")
    private val CAMERA_LABELS = listOf("camera", "caméra", "appareil photo")

    fun isSpotlight(root: AccessibilityNodeInfo): Boolean =
        NodeTools.isTabActive(root, SPOTLIGHT_LABELS)

    /** Clique sur l'onglet Caméra. Renvoie false si on ne l'a pas trouvé. */
    fun goToCamera(root: AccessibilityNodeInfo): Boolean =
        NodeTools.clickLabel(root, CAMERA_LABELS)
}
