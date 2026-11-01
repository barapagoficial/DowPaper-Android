package com.dowpaper.android.scenes

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import com.dowpaper.android.core.Col
import kotlin.math.max
import kotlin.math.sqrt

class NetworkScene(ctx: Context) : Scene(ctx) {
    private var xs = FloatArray(0); private var ys = FloatArray(0); private var vx = FloatArray(0); private var vy = FloatArray(0)
    private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val dotP = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private var tx = 0f; private var ty = 0f; private var touching = false
    private var maxD = 0f

    override fun setup() {
        val n = count(58)
        xs = FloatArray(n) { rf(0f, width) }; ys = FloatArray(n) { rf(0f, height) }
        vx = FloatArray(n) { rf(-1f, 1f) * dp(22f) }; vy = FloatArray(n) { rf(-1f, 1f) * dp(22f) }
        maxD = dp(128f)
        glow.shader = RadialGradient(width / 2, height * 0.45f, max(width, height) * 0.7f,
            intArrayOf(Col.alpha(color(2), 0.22f), Col.alpha(color(0), 0.06f), 0), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        lineP.strokeWidth = dp(1.1f)
    }

    override fun render(c: Canvas, t: Float, dt: Float) {
        val w = width; val h = height
        drawBackground(c)
        if (!overlay) c.drawRect(0f, 0f, w, h, glow)
        val n = xs.size
        val attract = dp(220f)
        for (i in 0 until n) {
            if (touching) {
                val dx = tx - xs[i]; val dy = ty - ys[i]
                val d = sqrt(dx * dx + dy * dy)
                if (d < attract && d > dp(30f)) { vx[i] += dx / d * dp(90f) * dt; vy[i] += dy / d * dp(90f) * dt }
            }
            val sp = sqrt(vx[i] * vx[i] + vy[i] * vy[i]); val lim = dp(80f)
            if (sp > lim) { vx[i] *= lim / sp; vy[i] *= lim / sp }
            if (!touching && sp > dp(30f)) { vx[i] *= 0.99f; vy[i] *= 0.99f }
            xs[i] += vx[i] * dt; ys[i] += vy[i] * dt
            if (xs[i] < 0) { xs[i] = 0f; vx[i] = -vx[i] } else if (xs[i] > w) { xs[i] = w; vx[i] = -vx[i] }
            if (ys[i] < 0) { ys[i] = 0f; vy[i] = -vy[i] } else if (ys[i] > h) { ys[i] = h; vy[i] = -vy[i] }
        }
        c.save(); c.translate(-px * w * 0.12f, 0f)
        val md2 = maxD * maxD
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val dx = xs[i] - xs[j]; val dy = ys[i] - ys[j]
                val d2 = dx * dx + dy * dy
                if (d2 < md2) {
                    val a = 1f - sqrt(d2) / maxD
                    lineP.color = Col.alpha(Col.lerp(color(0), color(1), xs[i] / w), a * 0.7f)
                    c.drawLine(xs[i], ys[i], xs[j], ys[j], lineP)
                }
            }
            if (touching) {
                val dx = xs[i] - tx; val dy = ys[i] - ty
                val d2 = dx * dx + dy * dy
                if (d2 < attract * attract) {
                    lineP.color = Col.alpha(Col.lighten(color(3), 0.3f), (1f - sqrt(d2) / attract) * 0.9f)
                    c.drawLine(xs[i], ys[i], tx, ty, lineP)
                }
            }
        }
        for (i in 0 until n) {
            dotP.color = Col.lighten(Col.lerp(color(0), color(1), xs[i] / w), 0.3f)
            c.drawCircle(xs[i], ys[i], dp(2.4f), dotP)
        }
        if (touching) { dotP.color = Color.WHITE; c.drawCircle(tx, ty, dp(4f), dotP) }
        c.restore()
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        tx = x + px * width * 0.12f; ty = y
        touching = action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL
    }
}
