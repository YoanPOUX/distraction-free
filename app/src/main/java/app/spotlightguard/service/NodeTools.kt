package app.spotlightguard.service

import android.view.accessibility.AccessibilityNodeInfo

/** Outils de parcours de l'arbre d'accessibilité, communs à Snapchat et Instagram. */
object NodeTools {

    private const val MAX_NODES = 800
    private const val MAX_DUMP_NODES = 300

    /** Textes et descriptions du nœud, en minuscules, sans les vides. */
    fun labels(node: AccessibilityNodeInfo): List<String> =
        listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

    /** Parcours en largeur, limité à [MAX_NODES] nœuds. */
    fun find(
        root: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.addLast(root)
        var seen = 0
        while (queue.isNotEmpty() && seen < MAX_NODES) {
            val node = queue.removeFirst()
            seen++
            if (predicate(node)) return node
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.addLast(it) }
            }
        }
        return null
    }

    /** Un onglet dont un libellé contient l'un des mots-clés, et qui est sélectionné. */
    fun isTabActive(root: AccessibilityNodeInfo, keywords: List<String>): Boolean =
        find(root) { node ->
            labels(node).any { label -> keywords.any { label.contains(it) } } && isActive(node)
        } != null

    /** Clique sur le premier élément dont le libellé correspond. Renvoie false s'il est introuvable. */
    fun clickLabel(root: AccessibilityNodeInfo, wanted: List<String>): Boolean {
        val node = find(root) { n ->
            labels(n).any { label -> wanted.any { matches(label, it) } }
        } ?: return false
        var target: AccessibilityNodeInfo? = node
        for (depth in 0..3) {
            val t = target ?: return false
            if (t.isClickable) return t.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            target = t.parent
        }
        return false
    }

    /** Arbre de vues lisible pour le mode diagnostic (textes tronqués, gardé en mémoire). */
    fun dump(root: AccessibilityNodeInfo, header: String): String {
        val sb = StringBuilder()
        sb.append(header).append('\n')

        fun walk(node: AccessibilityNodeInfo, depth: Int, budget: IntArray) {
            if (budget[0] <= 0) return
            budget[0]--
            sb.append("  ".repeat(depth))
            sb.append(node.className?.toString()?.substringAfterLast('.') ?: "?")
            node.viewIdResourceName?.let { sb.append(" #").append(it.substringAfter(":id/")) }
            node.text?.let { sb.append(" t=\"").append(it.take(40)).append('"') }
            node.contentDescription?.let { sb.append(" d=\"").append(it.take(40)).append('"') }
            if (node.isSelected) sb.append(" [sel]")
            if (node.isChecked) sb.append(" [chk]")
            if (node.isClickable) sb.append(" [clk]")
            sb.append('\n')
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { walk(it, depth + 1, budget) }
            }
        }

        walk(root, 0, intArrayOf(MAX_DUMP_NODES))
        return sb.toString()
    }

    private fun matches(label: String, wanted: String): Boolean =
        label == wanted || label.startsWith("$wanted,") || label.startsWith("$wanted ")

    /** Le nœud, ou l'un de ses 3 parents, est sélectionné / coché. */
    private fun isActive(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        for (depth in 0..3) {
            val c = current ?: return false
            if (c.isSelected || c.isChecked) return true
            current = c.parent
        }
        return false
    }
}
