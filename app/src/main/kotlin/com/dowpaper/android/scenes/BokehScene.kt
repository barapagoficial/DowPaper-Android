package com.dowpaper.android.scenes

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import com.dowpaper.android.core.Col
import kotlin.math.sin
import kotlin.math.sqrt

class BokehScene(ctx: Context) : Scene(ctx) {
    private class Orb(var x: Float, var y: Float, val r: Float, val vy: Float, val ph: Float,
                      val sh: RadialGradient, val p: Paint, val m: Matrix, var vx: Float = 0f)
    private val orbs = ArrayList<Orb>()

    override fun setup() {
        orbs.clear()
        val n = count(if (overlay) 26 else 38)
        for (i in 0 until n) {
            val c = color(i)
            val r = rf(dp(14f), dp(72f))
            val sh = RadialGradient(0f, 0f, r,
                intArrayOf(Col.alpha(c, 0.28f), Col.alpha(c, 0.32f), Col.alpha(c, 0.62f), Col.alpha(c, 0f)),
                floatArrayOf(0f, 0.72f, 0.92f, 1f), Shader.TileMode.CLAMP)
            orbs += Orb(rf(0f, width), rf(0f, height), r, rf(8f, 34f), rf(0f, 6.28f), sh,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = sh; xfermode = SCREEN }, Matrix())
        }
    }

    override fun render(c: Canvas, t: Float, dt: Float) {
        val w = width; val h = height
        drawBackground(c)
        for (o in orbs) {
            o.y -= dp(o.vy) * dt
            o.x += (sin(t * 0.35f + o.ph) * dp(8f) + o.vx) * dt
            o.vx *= 0.96f
            if (o.y < -o.r) { o.y = h + o.r; o.x = rf(0f, w) }
            if (o.x < -o.r) o.x = w + o.r else if (o.x > w + o.r) o.x = -o.r
            val dx = o.x - px * w * 0.2f * (o.r / dp(72f))
            o.m.setTranslate(dx, o.y); o.sh.setLocalMatrix(o.m)
            o.p.alpha = ((0.55f + 0.45f * sin(t * 0.8f + o.ph * 2f)) * 255).toInt()
            c.drawCircle(dx, o.y, o.r, o.p)
        }
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        if (action == MotionEvent.ACTION_UP) return
        val rad = dp(170f)
        for (o in orbs) {
            val dx = o.x - x; val dy = o.y - y
            val d = sqrt(dx * dx + dy * dy)
            if (d < rad && d > 1f) {
                val f = (1f - d / rad) * dp(520f)
                o.vx += dx / d * f * 0.2f
                o.y += dy / d * f * 0.01f
            }
        }
    }
}
