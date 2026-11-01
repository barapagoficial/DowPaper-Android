package com.dowpaper.android.scenes

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import com.dowpaper.android.core.Col
import com.dowpaper.android.core.Noise
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class FluidScene(ctx: Context) : Scene(ctx) {
    private class Blob(val paint: Paint, val shader: RadialGradient, val m: Matrix, val sx: Float, val sy: Float, val ph: Float)
    private val blobs = ArrayList<Blob>()
    private val vignette = Paint()
    private var tx = 0f; private var ty = 0f; private var tStr = 0f; private var touching = false

    override fun setup() {
        blobs.clear()
        val n = min(10, max(3, count(6)))
        val big = max(width, height)
        for (i in 0 until n) {
            val c = color(i)
            val r = big * rf(0.38f, 0.62f)
            val sh = RadialGradient(0f, 0f, r, intArrayOf(Col.alpha(c, 0.95f), Col.alpha(c, 0.45f), Col.alpha(c, 0f)),
                floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = sh; xfermode = SCREEN }
            blobs += Blob(p, sh, Matrix(), rf(0f, 100f), rf(0f, 100f), rf(0f, 6.28f))
        }
        vignette.shader = RadialGradient(width / 2, height / 2, big * 0.8f,
            intArrayOf(0, 0, Col.alpha(Color.BLACK, 0.55f)), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
    }

    override fun render(c: Canvas, t: Float, dt: Float) {
        val w = width; val h = height
        drawBackground(c)
        if (touching) tStr = min(1f, tStr + dt * 3f / speed.coerceAtLeast(0.2f)) else tStr *= 0.97f
        for ((i, b) in blobs.withIndex()) {
            var cx = w * (0.5f + 0.55f * Noise.n2(b.sx, t * 0.06f)) - px * w * 0.25f
            var cy = h * (0.5f + 0.55f * Noise.n2(b.sy, t * 0.06f + 3f))
            if (i == 0 && tStr > 0.01f) { cx += (tx - cx) * tStr * 0.85f; cy += (ty - cy) * tStr * 0.85f }
            val s = 1f + 0.12f * sin(t * 0.5f + b.ph)
            b.m.setScale(s, s); b.m.postTranslate(cx, cy)
            b.shader.setLocalMatrix(b.m)
            c.drawRect(0f, 0f, w, h, b.paint)
        }
        if (!overlay) c.drawRect(0f, 0f, w, h, vignette)
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        tx = x; ty = y
        touching = action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL
    }
}
