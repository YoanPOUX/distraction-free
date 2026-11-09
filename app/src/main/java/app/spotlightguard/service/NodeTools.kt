package app.spotlightguard.service

import android.view.accessibility.AccessibilityNodeInfo

/** Outils de parcours de l'arbre d'accessibilité, communs à Snapchat et Instagram. */
object NodeTools {

    private const val MAX_NODES = 1500
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

    /** Tous les nœuds (largeur d'abord, limité à [MAX_NODES]). */
    fun all(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val out = ArrayList<AccessibilityNodeInfo>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.addLast(root)
        while (queue.isNotEmpty() && out.size < MAX_NODES) {
            val node = queue.removeFirst()
            out.add(node)
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.addLast(it) }
            }
        }
        return out
    }

    fun viewId(node: AccessibilityNodeInfo): String =
        node.viewIdResourceName?.substringAfter(":id/")?.lowercase() ?: ""

    /** Le nœud est visible et occupe au moins [minHeightRatio] de la hauteur de la fenêtre. */
    fun isBig(node: AccessibilityNodeInfo, root: AccessibilityNodeInfo, minHeightRatio: Float): Boolean {
        if (!node.isVisibleToUser) return false
        val r = android.graphics.Rect()
        val w = android.graphics.Rect()
        node.getBoundsInScreen(r)
        root.getBoundsInScreen(w)
        return w.height() > 0 && r.height() >= w.height() * minHeightRatio
    }

    /** Sélectionné / coché : le nœud, ses enfants et petits-enfants, ou ses 2 parents. */
    fun isSelectedDeep(node: AccessibilityNodeInfo): Boolean {
        fun down(n: AccessibilityNodeInfo, depth: Int): Boolean {
            if (n.isSelected || n.isChecked) return true
            if (depth == 0) return false
            for (i in 0 until n.childCount) {
                val c = n.getChild(i) ?: continue
                if (down(c, depth - 1)) return true
            }
            return false
        }
        if (down(node, 2)) return true
        var p = node.parent
        for (depth in 0..1) {
            val c = p ?: return false
            if (c.isSelected || c.isChecked) return true
            p = c.parent
        }
        return false
    }

    /** Clique sur le nœud ou sur son premier parent cliquable (4 niveaux). */
    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        var target: AccessibilityNodeInfo? = node
        for (depth in 0..4) {
            val t = target ?: return false
            if (t.isClickable && t.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            target = t.parent
        }
        return false
    }

    /** Clique sur le premier élément dont l'identifiant se termine par l'un des suffixes. */
    fun clickViewId(root: AccessibilityNodeInfo, suffixes: List<String>): Boolean {
        for (node in all(root)) {
            val id = viewId(node)
            if (id.isNotEmpty() && suffixes.any { id == it }) {
                if (clickNode(node)) return true
            }
        }
        return false
    }

    /** Clique sur le premier élément dont le libellé correspond. Renvoie false s'il est introuvable. */
    fun clickLabel(root: AccessibilityNodeInfo, wanted: List<String>): Boolean {
        for (node in all(root)) {
            if (labels(node).any { label -> wanted.any { matches(label, it) } } && clickNode(node)) {
                return true
            }
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
            if (!node.isVisibleToUser) sb.append(" [hid]")
            val r = android.graphics.Rect()
            node.getBoundsInScreen(r)
            sb.append(" ").append(r.left).append(',').append(r.top).append('-').append(r.right).append(',').append(r.bottom)
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

    fun matches(label: String, wanted: String): Boolean =
        label == wanted || label.startsWith("$wanted,") || label.startsWith("$wanted ")
}
