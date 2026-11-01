package com.dowpaper.android.scenes

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import com.dowpaper.android.core.Col
import kotlin.math.max
import kotlin.math.min

class WarpScene(ctx: Context) : Scene(ctx) {
    private var xs = FloatArray(0); private var ys = FloatArray(0); private var zs = FloatArray(0)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowM = Matrix()
    private var glowSh: RadialGradient? = null
    private var cx = 0f; private var cy = 0f; private var tcx = 0f; private var tcy = 0f
    private var boost = 0f; private var touching = false

    override fun setup() {
        val n = count(240)
        xs = FloatArray(n) { rf(-1f, 1f) }; ys = FloatArray(n) { rf(-1f, 1f) }; zs = FloatArray(n) { rf(0.05f, 1f) }
        cx = width / 2; cy = height / 2; tcx = cx; tcy = cy
        val c = color(2)
        val sh = RadialGradient(0f, 0f, max(width, height) * 0.5f, intArrayOf(Col.alpha(c, 0.35f), Col.alpha(c, 0.08f), 0),
            floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP)
        glowSh = sh; glow.shader = sh; glow.xfermode = SCREEN
    }

    override fun render(c: Canvas, t: Float, dt: Float) {
        val w = width; val h = height
        drawBackground(c)
        if (!touching) { tcx = w / 2 - px * w * 0.5f; tcy = h / 2 }
        cx += (tcx - cx) * min(1f, dt * 3f); cy += (tcy - cy) * min(1f, dt * 3f)
        boost += ((if (touching) 2.2f else 0f) - boost) * min(1f, dt * 2f)
        glowM.setTranslate(cx, cy); glowSh?.setLocalMatrix(glowM)
        c.drawCircle(cx, cy, max(w, h) * 0.5f, glow)
        val f = min(w, h) * 0.5f
        val vz = 0.32f * (1f + boost)
        val tail = 0.03f + 0.09f * boost
        for (i in xs.indices) {
            zs[i] -= dt * vz
            if (zs[i] <= 0.02f) respawn(i)
            val z = zs[i]
            val sx = cx + xs[i] / z * f; val sy = cy + ys[i] / z * f
            if (sx < -50 || sx > w + 50 || sy < -50 || sy > h + 50) { respawn(i); continue }
            val tz = z + tail
            val tx = cx + xs[i] / tz * f; val ty = cy + ys[i] / tz * f
            val a = (1f - z).coerceIn(0f, 1f)
            line.color = Col.alpha(Col.lighten(color(i % 3), 0.35f), a)
            line.strokeWidth = dp(0.6f) + a * dp(2.4f)
            c.drawLine(tx, ty, sx, sy, line)
        }
    }

    private fun respawn(i: Int) {
        xs[i] = rf(-1f, 1f); ys[i] = rf(-1f, 1f); zs[i] = rf(0.8f, 1f)
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        touching = action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL
        if (touching) { tcx = x; tcy = y }
    }
}
