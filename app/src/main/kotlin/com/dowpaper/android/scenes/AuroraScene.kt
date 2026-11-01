package com.dowpaper.android.scenes

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import com.dowpaper.android.core.Col
import com.dowpaper.android.core.Noise
import kotlin.math.sin

class AuroraScene(ctx: Context) : Scene(ctx) {
    private val seg = 44
    private var sx = FloatArray(0); private var sy = FloatArray(0); private var sr = FloatArray(0); private var sp = FloatArray(0)
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val ribbon = Array(3) { Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = SCREEN } }
    private val rayP = Array(3) { Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; xfermode = SCREEN; strokeCap = Paint.Cap.ROUND } }
    private val path = Path()
    private val top = FloatArray(seg + 1)
    private val thick = FloatArray(seg + 1)
    private var rays = FloatArray(0)
    private val far = Path(); private val near = Path()
    private val farP = Paint(Paint.ANTI_ALIAS_FLAG); private val nearP = Paint(Paint.ANTI_ALIAS_FLAG)
    private val horizon = Paint()
    private var boost = 0f

    override fun setup() {
        val w = width; val h = height
        val n = count(150)
        sx = FloatArray(n) { rf(0f, w) }; sy = FloatArray(n) { rf(0f, h * 0.8f) }
        sr = FloatArray(n) { rf(dp(0.5f), dp(1.5f)) }; sp = FloatArray(n) { rf(0f, 6.28f) }
        for (i in 0..2) {
            val c = color(i)
            val base = h * (0.2f + 0.1f * i)
            ribbon[i].shader = LinearGradient(0f, base - h * 0.1f, 0f, base + h * 0.3f,
                intArrayOf(Col.alpha(c, 0f), Col.alpha(c, 0.55f), Col.alpha(c, 0.16f), Col.alpha(c, 0f)),
                floatArrayOf(0f, 0.28f, 0.62f, 1f), Shader.TileMode.CLAMP)
            rayP[i].shader = LinearGradient(0f, base - h * 0.1f, 0f, base + h * 0.3f,
                intArrayOf(Col.alpha(Col.lighten(c, 0.3f), 0f), Col.alpha(Col.lighten(c, 0.3f), 0.8f), Col.alpha(c, 0.1f), Col.alpha(c, 0f)),
                floatArrayOf(0f, 0.25f, 0.6f, 1f), Shader.TileMode.CLAMP)
            rayP[i].strokeWidth = dp(1.4f)
        }
        rays = FloatArray(count(70) * 4)
        buildMountain(far, h * 0.80f, h * 0.07f, 1.3f, 3f)
        buildMountain(near, h * 0.88f, h * 0.06f, 2.1f, 11f)
        farP.color = Col.darken(palette.bgBottom, 0.35f)
        nearP.color = Col.darken(palette.bgBottom, 0.7f)
        horizon.shader = LinearGradient(0f, h * 0.6f, 0f, h * 0.85f,
            Col.alpha(color(0), 0f), Col.alpha(color(0), 0.18f), Shader.TileMode.CLAMP)
    }

    private fun buildMountain(p: Path, base: Float, amp: Float, freq: Float, seed: Float) {
        val w = width; val h = height
        p.reset()
        p.moveTo(-w * 0.2f, h)
        val steps = 60
        for (k in 0..steps) {
            val x = -w * 0.2f + w * 1.4f * k / steps
            val nx = x / w
            val y = base - amp * (Noise.n1(nx * freq * 3f + seed) * 0.7f + Noise.n1(nx * freq * 9f + seed * 2f) * 0.3f + 0.5f)
            p.lineTo(x, y)
        }
        p.lineTo(w * 1.2f, h)
        p.close()
    }

    override fun render(c: Canvas, t: Float, dt: Float) {
        val w = width; val h = height
        boost *= 0.97f
        time += dt * boost * 1.5f
        drawBackground(c)
        val sShift = px * w * 0.04f
        for (i in sx.indices) {
            val tw = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * 1.3f + sp[i]))
            val fade = 1f - sy[i] / (h * 0.85f)
            starPaint.alpha = (255 * tw * fade).toInt().coerceIn(0, 255)
            var x = sx[i] - sShift
            if (x < 0) x += w else if (x > w) x -= w
            c.drawCircle(x, sy[i], sr[i], starPaint)
        }
        c.drawRect(0f, h * 0.6f, w, h, horizon)
        val shift = px * w * 0.12f
        val nr = rays.size / 4
        for (i in 0..2) {
            val base = h * (0.2f + 0.1f * i)
            val ts = t * (0.22f + 0.06f * i)
            for (k in 0..seg) {
                val nx = k.toFloat() / seg
                top[k] = base + sin(nx * 3.1f * 2f + ts * 1.3f + i) * h * 0.035f + Noise.n2(nx * 2.2f + i * 7f, ts * 0.7f) * h * 0.075f
                thick[k] = h * (0.09f + 0.08f * (0.5f + 0.5f * Noise.n2(nx * 2.8f + 3f + i * 4f, ts * 0.5f)))
            }
            path.reset()
            for (k in 0..seg) {
                val x = -w * 0.1f + w * 1.2f * k / seg - shift
                if (k == 0) path.moveTo(x, top[k]) else path.lineTo(x, top[k])
            }
            for (k in seg downTo 0) {
                val x = -w * 0.1f + w * 1.2f * k / seg - shift
                path.lineTo(x, top[k] + thick[k])
            }
            path.close()
            ribbon[i].alpha = (200 + 55 * boost.coerceAtMost(1f)).toInt()
            c.drawPath(path, ribbon[i])
            for (r in 0 until nr) {
                val f = r.toFloat() / (nr - 1).coerceAtLeast(1)
                val kf = f * seg
                val k = kf.toInt().coerceAtMost(seg - 1)
                val fr = kf - k
                val x = -w * 0.1f + w * 1.2f * f - shift
                val y0 = top[k] + (top[k + 1] - top[k]) * fr
                val th = thick[k] + (thick[k + 1] - thick[k]) * fr
                val fl = 0.5f + 0.5f * sin(t * 2.2f + r * 1.7f + i * 2f)
                rays[r * 4] = x; rays[r * 4 + 1] = y0 + th * 0.05f
                rays[r * 4 + 2] = x; rays[r * 4 + 3] = y0 + th * (0.35f + 0.6f * fl)
            }
            rayP[i].alpha = (120 + 135 * boost.coerceAtMost(1f)).toInt()
            c.drawLines(rays, rayP[i])
        }
        c.save(); c.translate(-px * w * 0.08f, 0f); c.drawPath(far, farP); c.restore()
        c.save(); c.translate(-px * w * 0.16f, 0f); c.drawPath(near, nearP); c.restore()
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        if (action == MotionEvent.ACTION_DOWN) boost = 1.4f
        else if (action == MotionEvent.ACTION_MOVE) boost = (boost + 0.05f).coerceAtMost(1.4f)
    }
}
