package com.dowpaper.android.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.dowpaper.android.core.dp

object T {
    const val BG = 0xFF07070D.toInt()
    const val TEXT = Color.WHITE
    const val TEXT2 = 0xB8FFFFFF.toInt()
    const val TEXT3 = 0x80FFFFFF.toInt()
    const val GLASS = 0x14FFFFFF
    const val GLASS2 = 0x22FFFFFF
    const val STROKE = 0x1FFFFFFF
    const val PANEL = 0xF20D0C18.toInt()
    const val SCRIM = 0x66000000
    const val A1 = 0xFF8B5CFF.toInt()
    const val A2 = 0xFF4F7BFF.toInt()
    const val A3 = 0xFF00E0FF.toInt()
    const val PINK = 0xFFFF5CA8.toInt()
    val GRAD = intArrayOf(A1, A2, A3)

    val black: Typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
    val bold: Typeface = Typeface.create("sans-serif", Typeface.BOLD)
    val medium: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    val regular: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
}

fun Context.label(text: String, sizeSp: Float, color: Int = T.TEXT, tf: Typeface = T.regular): TextView =
    TextView(this).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(color)
        typeface = tf
        includeFontPadding = false
    }

fun Context.glass(radiusDp: Float, fill: Int = T.GLASS, stroke: Int = T.STROKE): GradientDrawable =
    GradientDrawable().apply {
        cornerRadius = dp(radiusDp)
        setColor(fill)
        setStroke(dp(1f).toInt().coerceAtLeast(1), stroke)
    }

fun lp(w: Int, h: Int) = ViewGroup.LayoutParams(w, h)
const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

fun llp(w: Int, h: Int, weight: Float = 0f, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0) =
    LinearLayout.LayoutParams(w, h, weight).apply { setMargins(l, t, r, b) }

fun flp(w: Int, h: Int, gravity: Int, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0) =
    FrameLayout.LayoutParams(w, h, gravity).apply { setMargins(l, t, r, b) }

class RoundOutline(private val r: Float) : ViewOutlineProvider() {
    override fun getOutline(view: View, outline: Outline) {
        outline.setRoundRect(0, 0, view.width, view.height, r)
    }
}

fun View.roundClip(r: Float) {
    outlineProvider = RoundOutline(r)
    clipToOutline = true
}

/** Animación de presión elástica para cualquier vista clicable. */
fun View.pressable(scale: Float = 0.95f) {
    setOnTouchListener { v, e ->
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> v.animate().scaleX(scale).scaleY(scale).setDuration(110).setInterpolator(null).start()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(320).setInterpolator(OvershootInterpolator(3f)).start()
        }
        false
    }
}
