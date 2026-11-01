package com.dowpaper.android.scenes

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import com.dowpaper.android.core.Col
import kotlin.math.max

class RainScene(ctx: Context) : Scene(ctx) {
    private var xs = FloatArray(0); private var ys = FloatArray(0); private var ls = FloatArray(0); private var vs = FloatArray(0)
    private var lines = FloatArray(0)
    private val dropP = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val splashP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val city = Path()
    private val cityP = Paint(Paint.ANTI_ALIAS_FLAG)
    private val winP = Paint()
    private var wins = FloatArray(0)
    private val lights = ArrayList<Paint>()
    private var lx = FloatArray(0); private var ly = FloatArray(0); private var lr = FloatArray(0)
    private val sx = FloatArray(40); private val sy = FloatArray(40); private val sa = FloatArray(40)
    private var si = 0
    private var flash = 0f; private var nextFlash = 8f
    private val flashP = Paint()
    private val slant = 0.18f

    override fun setup() {
        val w = width; val h = height
        val n = count(if (overlay) 110 else 150)
        xs = FloatArray(n) { rf(-w * 0.2f, w) }; ys = FloatArray(n) { rf(-h, h) }
        ls = FloatArray(n) { rf(dp(12f), dp(30f)) }; vs = FloatArray(n) { rf(dp(700f), dp(1100f)) }
        lines = FloatArray(n * 4)
        dropP.color = Col.alpha(Col.lighten(color(2), 0.6f), 0.45f)
        dropP.strokeWidth = dp(1.2f)
        city.reset(); city.moveTo(0f, h)
        val wl = ArrayList<Float>()
        var x = 0f
        while (x < w) {
            val bw = rf(dp(26f), dp(70f)); val bh = rf(h * 0.12f, h * 0.38f)
            city.lineTo(x, h - bh); city.lineTo(x + bw, h - bh)
            var wy = h - bh + dp(10f)
            while (wy < h - dp(10f)) {
                var wx = x + dp(6f)
                while (wx < x + bw - dp(8f)) {
                    if (rnd.nextFloat() < 0.28f) { wl.add(wx); wl.add(wy) }
                    wx += dp(9f)
                }
                wy += dp(13f)
            }
            x += bw
        }
        city.lineTo(w, h); city.close()
        wins = FloatArray(wl.size) { wl[it] }
        cityP.color = Col.darken(palette.bgTop, 0.4f)
        winP.color = Col.alpha(Col.lighten(color(1), 0.4f), 0.55f)
        lights.clear()
        val nl = 7
        lx = FloatArray(nl) { rf(0f, w) }; ly = FloatArray(nl) { rf(h * 0.55f, h * 0.95f) }; lr = FloatArray(nl) { rf(w * 0.2f, w * 0.45f) }
        for (i in 0 until nl) {
            val c = color(i)
            lights += Paint().apply {
                shader = RadialGradient(lx[i], ly[i], lr[i], intArrayOf(Col.alpha(c, 0.32f), 0), null, Shader.TileMode.CLAMP)
                xfermode = SCREEN
            }
        }
        flashP.color = Col.lighten(color(2), 0.8f)
    }

    override fun render(c: Canvas, t: Float, dt: Float) {
        val w = width; val h = height
        if (!overlay) {
            drawBackground(c)
            for ((i, l) in lights.withIndex()) c.drawCircle(lx[i], ly[i], lr[i], l)
            c.save(); c.translate(-px * w * 0.05f, 0f)
            c.drawPath(city, cityP)
            var k = 0
            while (k < wins.size) {
                val on = ((k * 7919 + (t * 0.2f).toInt()) % 11) != 0
                if (on) c.drawRect(wins[k], wins[k + 1], wins[k] + dp(4f), wins[k + 1] + dp(6f), winP)
                k += 2
            }
            c.restore()
            nextFlash -= dt
            if (nextFlash < 0) { flash = 1f; nextFlash = rf(7f, 16f) }
            if (flash > 0.01f) {
                flashP.alpha = (flash * flash * 90).toInt()
                c.drawRect(0f, 0f, w, h, flashP)
                flash *= 0.9f
            }
        }
        for (i in xs.indices) {
            ys[i] += vs[i] * dt; xs[i] += vs[i] * slant * dt
            val ground = if (overlay) h else h * 0.97f
            if (ys[i] > ground) {
                sx[si] = xs[i]; sy[si] = ground; sa[si] = 1f; si = (si + 1) % sx.size
                ys[i] = rf(-h * 0.3f, -dp(20f)); xs[i] = rf(-w * 0.2f, w)
            }
            lines[i * 4] = xs[i]; lines[i * 4 + 1] = ys[i]
            lines[i * 4 + 2] = xs[i] - ls[i] * slant; lines[i * 4 + 3] = ys[i] - ls[i]
        }
        c.drawLines(lines, dropP)
        splashP.strokeWidth = dp(1f)
        for (i in sx.indices) {
            if (sa[i] <= 0f) continue
            sa[i] -= dt * 3f
            val a = max(0f, sa[i])
            splashP.color = Col.alpha(Col.lighten(color(2), 0.6f), a * 0.5f)
            val r = dp(10f) * (1f - a) + dp(2f)
            c.drawOval(sx[i] - r, sy[i] - r * 0.3f, sx[i] + r, sy[i] + r * 0.3f, splashP)
        }
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        if (action == MotionEvent.ACTION_DOWN) flash = 1f
    }
}
