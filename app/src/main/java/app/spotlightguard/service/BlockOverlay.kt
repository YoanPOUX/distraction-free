package app.spotlightguard.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import app.spotlightguard.R

/**
 * Écran de blocage minimaliste, posé par-dessus Snapchat.
 * Fenêtre de type « overlay d'accessibilité » : aucune permission supplémentaire.
 * Les couleurs viennent de res/values(-night)/colors.xml, donc clair/sombre automatique.
 */
class BlockOverlay(private val service: AccessibilityService) {

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var view: View? = null

    /** true tant que l'utilisateur doit choisir une action (mode souple). */
    var persistent: Boolean = false
        private set

    val isShowing: Boolean get() = view != null

    fun show(
        title: String,
        subtitle: String,
        persistent: Boolean,
        actions: List<Pair<String, () -> Unit>> = emptyList(),
        autoHideMs: Long = 0L,
    ) {
        if (view != null) return
        this.persistent = persistent

        val fg = ContextCompat.getColor(service, R.color.fg)
        val fgSecondary = ContextCompat.getColor(service, R.color.fg_secondary)
        val outline = ContextCompat.getColor(service, R.color.outline)
        val overlayColor = ContextCompat.getColor(service, R.color.overlay_bg)
        val density = service.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val column = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(32), 0, dp(32), 0)
        }

        column.addView(
            TextView(service).apply {
                text = title
                setTextColor(fg)
                textSize = 26f
                typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                gravity = Gravity.CENTER
            },
        )

        column.addView(
            TextView(service).apply {
                text = subtitle
                setTextColor(fgSecondary)
                textSize = 15f
                gravity = Gravity.CENTER
                setPadding(0, dp(12), 0, if (actions.isEmpty()) 0 else dp(28))
            },
        )

        actions.forEach { (label, onClick) ->
            val button = TextView(service).apply {
                text = label
                setTextColor(fg)
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(dp(24), dp(14), dp(24), dp(14))
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setStroke(dp(1), outline)
                }
                setOnClickListener { onClick() }
            }
            column.addView(
                button,
                LinearLayout.LayoutParams(dp(240), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(10)
                },
            )
        }

        val root = FrameLayout(service).apply {
            setBackgroundColor(overlayColor)
            isClickable = true
            addView(
                column,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                ),
            )
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )

        try {
            windowManager.addView(root, params)
            view = root
        } catch (e: RuntimeException) {
            view = null
            return
        }

        if (autoHideMs > 0L) {
            handler.postDelayed({ hide() }, autoHideMs)
        }
    }

    fun hide() {
        handler.removeCallbacksAndMessages(null)
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
        persistent = false
    }
}
