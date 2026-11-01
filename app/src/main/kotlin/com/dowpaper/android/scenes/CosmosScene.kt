package com.dowpaper.android.scenes

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import com.dowpaper.android.core.Col
import com.dowpaper.android.core.Noise
import kotlin.math.*

class CosmosScene(ctx: Context) : Scene(ctx) {
    private class Cloud(val paint: Paint, val sh: RadialGradient, val m: Matrix, val r: Float, val seed: Float)
    private class Shoot(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val col: Int)
    private class Ring(var x: Float, var y: Float, var age: Float)

    private val clouds = ArrayList<Cloud>()
    private val layers = 3
    private val sx = arrayOfNulls<FloatArray>(layers)
    private val sy = arrayOfNulls<FloatArray>(layers)
    private val sr = arrayOfNulls<FloatArray>(layers)
    private val sp = arrayOfNulls<FloatArray>(layers)
    private val depth = floatArrayOf(0.02f, 0.06f, 0.12f)
    private val drift = floatArrayOf(2f, 5f, 10f)
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shootPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val shoots = ArrayList<Shoot>()
    private val rings = ArrayList<Ring>()
    private var spawn = 2f

    override fun setup() {
        clouds.clear(); shoots.clear(); rings.clear()
        val big = max(width, height)
        for (i in 0 until 5) {
            val c = color(i)
            val r = big * rf(0.22f, 0.4f)
            val sh = RadialGradient(0f, 0f, r, intArrayOf(Col.alpha(c, 0.42f), Col.alpha(c, 0.1f), Col.alpha(c, 0f)),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            clouds += Cloud(Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = sh; xfermode = SCREEN }, sh, Matrix(), r, rf(0f, 50f))
        }
        val base = intArrayOf(90, 60, 35)
        for (l in 0 until layers) {
            val n = count(base[l])
            sx[l] = FloatArray(n) { rf(0f, width) }
            sy[l] = FloatArray(n) { rf(0f, height) }
            sr[l] = FloatArray(n) { rf(dp(0.4f + l * 0.4f), dp(0.9f + l * 0.6f)) }
            sp[l] = FloatArray(n) { rf(0f, 6.28f) }
        }
    }

    override fun render(c: Canvas, t: Float, dt: Float) {
        val w = width; val h = height
        drawBackground(c)
        if (!overlay) for (cl in clouds) {
            val cx = w * (0.5f + 0.5f * Noise.n2(cl.seed, t * 0.025f)) - px * w * 0.1f
            val cy = h * (0.45f + 0.5f * Noise.n2(cl.seed + 20f, t * 0.025f))
            cl.m.setTranslate(cx, cy); cl.sh.setLocalMatrix(cl.m)
            c.drawCircle(cx, cy, cl.r, cl.paint)
        }
        for (l in 0 until layers) {
            val xs = sx[l]!!; val ys = sy[l]!!; val rs = sr[l]!!; val ps = sp[l]!!
            val off = px * w * depth[l] * 4f + t * dp(drift[l])
            for (i in xs.indices) {
                var x = (xs[i] - off) % w
                if (x < 0) x += w
                val tw = 0.4f + 0.6f * (0.5f + 0.5f * sin(t * (1f + l) + ps[i]))
                starPaint.color = if (i % 7 == 0) Col.lighten(color(i), 0.5f) else Color.WHITE
                starPaint.alpha = (tw * (150 + l * 50)).toInt().coerceIn(0, 255)
                c.drawCircle(x, ys[i], rs[i], starPaint)
            }
        }
        spawn -= dt
        if (spawn <= 0f) {
            spawn = rf(2.5f, 6f)
            val ang = rf(0.3f, 0.7f)
            addShoot(rf(0f, w), rf(0f, h * 0.5f), ang)
        }
        val it = shoots.iterator()
        while (it.hasNext()) {
            val s = it.next()
            s.x += s.vx * dt; s.y += s.vy * dt; s.life -= dt
            if (s.life <= 0f) { it.remove(); continue }
            val a = min(1f, s.life * 2f)
            for (k in 0 until 12) {
                val f0 = k / 12f; val f1 = (k + 1) / 12f
                shootPaint.color = Col.alpha(s.col, a * (1f - f0) * 0.9f)
                shootPaint.strokeWidth = dp(2.2f) * (1f - f0)
                c.drawLine(s.x - s.vx * 0.12f * f0, s.y - s.vy * 0.12f * f0, s.x - s.vx * 0.12f * f1, s.y - s.vy * 0.12f * f1, shootPaint)
            }
        }
        val ri = rings.iterator()
        while (ri.hasNext()) {
            val r = ri.next()
            r.age += dt
            if (r.age > 1.6f) { ri.remove(); continue }
            ringPaint.color = Col.alpha(color(1), (1f - r.age / 1.6f) * 0.7f)
            ringPaint.strokeWidth = dp(2f)
            c.drawCircle(r.x, r.y, dp(20f) + r.age * dp(140f), ringPaint)
        }
    }

    private fun addShoot(x: Float, y: Float, ang: Float) {
        if (shoots.size > 6) return
        val v = dp(rf(500f, 800f))
        val dir = if (rnd.nextBoolean()) 1f else -1f
        shoots += Shoot(x, y, cos(ang) * v * dir, sin(ang) * v, rf(0.8f, 1.3f), Col.lighten(color(rnd.nextInt(5)), 0.6f))
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        if (action == MotionEvent.ACTION_DOWN) {
            rings += Ring(x, y, 0f)
            addShoot(x, y, rf(0.2f, 0.9f))
        }
    }
}
