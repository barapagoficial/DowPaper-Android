package com.dowpaper.android.scenes

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import com.dowpaper.android.core.Col
import com.dowpaper.android.core.Noise
import kotlin.math.sin

class SnowScene(ctx: Context) : Scene(ctx) {
    private var xs = FloatArray(0); private var ys = FloatArray(0); private var rs = FloatArray(0)
    private var vys = FloatArray(0); private var ph = FloatArray(0); private var dz = FloatArray(0)
    private val flake = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hills = Array(2) { Path() }
    private val hillP = Array(2) { Paint(Paint.ANTI_ALIAS_FLAG) }
    private val glow = Paint()
    private var wind = 0f

    override fun setup() {
        val w = width; val h = height
        val n = count(if (overlay) 110 else 150)
        xs = FloatArray(n) { rf(0f, w) }; ys = FloatArray(n) { rf(0f, h) }
        dz = FloatArray(n) { rf(0.2f, 1f) }
        rs = FloatArray(n) { dp(0.8f) + dz[it] * dp(2.8f) }
        vys = FloatArray(n) { dp(20f) + dz[it] * dp(55f) }
        ph = FloatArray(n) { rf(0f, 6.28f) }
        val c = color(4)
        glow.shader = RadialGradient(w * 0.3f, h * 0.2f, w * 0.9f, intArrayOf(Col.alpha(color(1), 0.28f), 0), null, Shader.TileMode.CLAMP)
        for (l in 0..1) {
            val p = hills[l]; p.reset(); p.moveTo(-w * 0.2f, h)
            for (k in 0..50) {
                val x = -w * 0.2f + w * 1.4f * k / 50
                p.lineTo(x, h * (0.82f + 0.08f * l) - h * 0.05f * (0.5f + Noise.n1(x / w * 2.5f + l * 9f)))
            }
            p.lineTo(w * 1.2f, h); p.close()
            hillP[l].color = Col.lerp(Col.lerp(palette.bgBottom, c, 0.25f + 0.2f * l), Color.WHITE, 0.1f * l)
        }
    }

    override fun render(c: Canvas, t: Float, dt: Float) {
        val w = width; val h = height
        wind *= 0.985f
        if (!overlay) {
            drawBackground(c)
            c.drawRect(0f, 0f, w, h, glow)
            c.save(); c.translate(-px * w * 0.06f, 0f); c.drawPath(hills[0], hillP[0]); c.restore()
        }
        val fc = Col.lighten(color(4), 0.6f)
        for (i in xs.indices) {
            ys[i] += vys[i] * dt
            xs[i] += (sin(t * 0.9f + ph[i]) * dp(16f) + wind) * dt * dz[i]
            if (ys[i] > h + 10) { ys[i] = -10f; xs[i] = rf(0f, w) }
            if (xs[i] < -10) xs[i] = w + 10 else if (xs[i] > w + 10) xs[i] = -10f
            flake.color = Col.alpha(fc, 0.35f + 0.6f * dz[i])
            c.drawCircle(xs[i] - px * w * 0.15f * dz[i], ys[i], rs[i], flake)
            if (i == xs.size / 2 && !overlay) {
                c.save(); c.translate(-px * w * 0.12f, 0f); c.drawPath(hills[1], hillP[1]); c.restore()
            }
        }
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE)
            wind = (x - width / 2) / width * dp(600f)
    }
}
