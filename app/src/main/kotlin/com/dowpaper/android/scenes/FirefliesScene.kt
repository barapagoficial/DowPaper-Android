package com.dowpaper.android.scenes

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import com.dowpaper.android.core.Col
import com.dowpaper.android.core.Noise
import com.dowpaper.android.core.smooth
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class FirefliesScene(ctx: Context) : Scene(ctx) {
    private var bx = FloatArray(0); private var by = FloatArray(0); private var seed = FloatArray(0)
    private var ph = FloatArray(0); private var fq = FloatArray(0); private var depthA = BooleanArray(0)
    private val glowSh = arrayOfNulls<RadialGradient>(2)
    private val glowP = Array(2) { Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = SCREEN } }
    private val core = Paint(Paint.ANTI_ALIAS_FLAG)
    private val m = Matrix()
    private val moonGlow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val moon = Paint(Paint.ANTI_ALIAS_FLAG)
    private val star = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val hills = Array(3) { Path() }
    private val hillP = Array(3) { Paint(Paint.ANTI_ALIAS_FLAG) }
    private val trees = Path()
    private var stars = FloatArray(0)
    private var ax = 0f; private var ay = 0f; private var aStr = 0f; private var touching = false

    override fun setup() {
        val w = width; val h = height
        val n = count(if (overlay) 30 else 40)
        bx = FloatArray(n) { rf(0f, w) }
        by = FloatArray(n) { if (overlay) rf(h * 0.1f, h * 0.95f) else rf(h * 0.35f, h * 0.92f) }
        seed = FloatArray(n) { rf(0f, 200f) }; ph = FloatArray(n) { rf(0f, 6.28f) }; fq = FloatArray(n) { rf(0.6f, 1.6f) }
        depthA = BooleanArray(n) { rnd.nextFloat() < 0.4f }
        for (i in 0..1) {
            val c = Col.lighten(color(i), 0.15f)
            val sh = RadialGradient(0f, 0f, dp(28f), intArrayOf(Col.alpha(c, 0.9f), Col.alpha(c, 0.35f), Col.alpha(c, 0f)),
                floatArrayOf(0f, 0.22f, 1f), Shader.TileMode.CLAMP)
            glowSh[i] = sh; glowP[i].shader = sh
        }
        core.color = Col.lighten(color(1), 0.7f)
        val mc = Col.lighten(color(4), 0.5f)
        moonGlow.shader = RadialGradient(w * 0.74f, h * 0.15f, w * 0.45f, intArrayOf(Col.alpha(mc, 0.35f), Col.alpha(mc, 0.06f), 0),
            floatArrayOf(0f, 0.3f, 1f), Shader.TileMode.CLAMP)
        moon.color = mc
        val ns = count(50)
        stars = FloatArray(ns * 3)
        for (i in 0 until ns) { stars[i * 3] = rf(0f, w); stars[i * 3 + 1] = rf(0f, h * 0.5f); stars[i * 3 + 2] = rf(0f, 6.28f) }
        val bases = floatArrayOf(0.7f, 0.8f, 0.9f)
        val amps = floatArrayOf(0.08f, 0.06f, 0.05f)
        for (l in 0..2) {
            val p = hills[l]; p.reset(); p.moveTo(-w * 0.2f, h)
            for (k in 0..60) {
                val x = -w * 0.2f + w * 1.4f * k / 60
                val y = h * bases[l] - h * amps[l] * (0.5f + Noise.n1(x / w * (2f + l) + l * 13f) * 0.8f)
                p.lineTo(x, y)
            }
            p.lineTo(w * 1.2f, h); p.close()
            hillP[l].color = Col.darken(Col.lerp(palette.bgBottom, color(2), 0.15f), 0.35f + l * 0.25f)
        }
        trees.reset()
        var x = -w * 0.15f
        while (x < w * 1.15f) {
            val y = h * 0.8f - h * 0.06f * (0.5f + Noise.n1(x / w * 3f + 13f) * 0.8f) + dp(4f)
            val th = rf(dp(40f), dp(95f)); val tw = th * 0.42f
            for (s in 0 until 3) {
                val ty = y - th * s * 0.28f
                trees.moveTo(x, ty - th * 0.5f)
                trees.lineTo(x + tw * (1f - s * 0.2f) / 2f, ty)
                trees.lineTo(x - tw * (1f - s * 0.2f) / 2f, ty)
                trees.close()
            }
            trees.addRect(x - dp(2f), y - dp(2f), x + dp(2f), y + dp(10f), Path.Direction.CW)
            x += rf(dp(18f), dp(60f))
        }
    }

    override fun render(c: Canvas, t: Float, dt: Float) {
        val w = width; val h = height
        if (touching) aStr = min(1f, aStr + dt * 2f) else aStr *= 0.985f
        if (!overlay) {
            drawBackground(c)
            c.drawRect(0f, 0f, w, h, moonGlow)
            for (i in 0 until stars.size / 3) {
                star.alpha = ((0.3f + 0.7f * (0.5f + 0.5f * sin(t * 1.4f + stars[i * 3 + 2]))) * 180).toInt()
                c.drawCircle(stars[i * 3], stars[i * 3 + 1], dp(0.9f), star)
            }
            c.drawCircle(w * 0.74f, h * 0.15f, dp(30f), moon)
            c.save(); c.translate(-px * w * 0.05f, 0f); c.drawPath(hills[0], hillP[0]); c.restore()
            c.save(); c.translate(-px * w * 0.1f, 0f); c.drawPath(hills[1], hillP[1]); c.drawPath(trees, hillP[1]); c.restore()
        }
        drawFlies(c, t, true)
        if (!overlay) { c.save(); c.translate(-px * w * 0.18f, 0f); c.drawPath(hills[2], hillP[2]); c.restore() }
        drawFlies(c, t, false)
    }

    private fun drawFlies(c: Canvas, t: Float, back: Boolean) {
        val w = width; val h = height
        for (i in bx.indices) {
            if (depthA[i] != back) continue
            var x = bx[i] + Noise.n2(seed[i], t * 0.09f) * w * 0.28f - px * w * 0.12f
            var y = by[i] + Noise.n2(seed[i] + 50f, t * 0.09f) * h * 0.1f
            if (aStr > 0.01f) {
                val ox = ax + cos(seed[i] + t * 0.9f) * dp(30f + (i % 5) * 14f)
                val oy = ay + sin(seed[i] * 1.3f + t * 0.9f) * dp(30f + (i % 5) * 14f)
                x += (ox - x) * aStr * 0.8f; y += (oy - y) * aStr * 0.8f
            }
            val a = smooth(0.15f, 1f, 0.5f + 0.5f * sin(t * fq[i] * 1.6f + ph[i]))
            if (a < 0.02f) continue
            val s = if (back) 0.7f else 1f
            val k = i and 1
            m.setScale(s, s); m.postTranslate(x, y); glowSh[k]?.setLocalMatrix(m)
            glowP[k].alpha = (a * 255).toInt()
            c.drawCircle(x, y, dp(28f) * s, glowP[k])
            core.alpha = (a * 255).toInt()
            c.drawCircle(x, y, dp(1.8f) * s, core)
        }
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        ax = x; ay = y
        touching = action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL
    }
}
