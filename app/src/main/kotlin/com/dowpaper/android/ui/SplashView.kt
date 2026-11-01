package com.dowpaper.android.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.animation.LinearInterpolator
import com.dowpaper.android.R
import com.dowpaper.android.core.Col
import com.dowpaper.android.core.dp
import com.dowpaper.android.core.smooth
import kotlin.math.min

/** Intro animada: anillo de luz girando, logo que emerge y nombre con espaciado elástico. */
class SplashView(ctx: Context) : View(ctx) {
    private val bg = Paint().apply { color = T.BG }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = T.black }
    private val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = T.TEXT2; textAlign = Paint.Align.CENTER; typeface = T.medium }
    private val icon = ctx.getDrawable(R.mipmap.ic_launcher)
    private val rf = RectF()
    private val rm = Matrix()
    private var sweep: SweepGradient? = null
    private var t = 0f
    private val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
        val t0 = System.nanoTime()
        addUpdateListener { t = (System.nanoTime() - t0) / 1e9f; invalidate() }
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        glow.shader = RadialGradient(w / 2f, h * 0.44f, w * 0.7f, intArrayOf(Col.alpha(T.A1, 0.45f), Col.alpha(T.A3, 0.12f), 0),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        sweep = SweepGradient(w / 2f, h * 0.44f, intArrayOf(0, T.A1, T.A3, T.PINK, 0), floatArrayOf(0f, 0.3f, 0.6f, 0.85f, 1f))
        ring.shader = sweep
        title.textSize = context.dp(40f)
        sub.textSize = context.dp(15f)
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); anim.start() }
    override fun onDetachedFromWindow() { anim.cancel(); super.onDetachedFromWindow() }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2; val cy = h * 0.44f
        c.drawRect(0f, 0f, w, h, bg)
        val g = smooth(0f, 0.8f, t)
        glow.alpha = (g * 255).toInt()
        c.drawRect(0f, 0f, w, h, glow)
        val r = min(w, h) * 0.2f
        rm.setRotate(t * 240f, cx, cy); sweep?.setLocalMatrix(rm)
        val ra = smooth(0.05f, 0.6f, t)
        for (k in 0..2) {
            val rr = r * (1f + k * 0.18f) * (0.7f + 0.3f * ra)
            ring.strokeWidth = context.dp(3f - k * 0.8f)
            ring.alpha = (ra * (255 - k * 70)).toInt()
            rf.set(cx - rr, cy - rr, cx + rr, cy + rr)
            c.drawArc(rf, t * (200f + k * 60f) * (if (k % 2 == 0) 1 else -1), 260f - k * 40f, false, ring)
        }
        val ia = smooth(0.15f, 0.7f, t)
        val s = r * 0.8f * (0.6f + 0.4f * ia)
        icon?.let {
            it.setBounds((cx - s).toInt(), (cy - s).toInt(), (cx + s).toInt(), (cy + s).toInt())
            it.alpha = (ia * 255).toInt()
            it.draw(c)
        }
        val ta = smooth(0.45f, 1.0f, t)
        title.alpha = (ta * 255).toInt()
        title.letterSpacing = 0.3f * (1f - ta) + 0.01f
        c.drawText("DowPaper", cx, cy + r * 1.75f + (1f - ta) * context.dp(18f), title)
        val sa = smooth(0.7f, 1.2f, t)
        sub.alpha = (sa * 255).toInt()
        c.drawText("Fondos que cobran vida", cx, cy + r * 1.75f + context.dp(34f), sub)
    }
}
