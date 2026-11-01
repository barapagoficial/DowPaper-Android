package com.dowpaper.android.scenes

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import com.dowpaper.android.core.Col
import kotlin.math.*

class MandalaScene(ctx: Context) : Scene(ctx) {
    private val sides = intArrayOf(3, 6, 4, 8, 5, 12, 6, 10, 7, 16)
    private val rings get() = sides.size
    private val rot = FloatArray(10)
    private val path = Path()
    private val glowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; xfermode = SCREEN; strokeJoin = Paint.Join.ROUND }
    private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND }
    private val dotP = Paint(Paint.ANTI_ALIAS_FLAG)
    private val center = Paint(Paint.ANTI_ALIAS_FLAG)
    private var dustA = FloatArray(0); private var dustR = FloatArray(0); private var dustW = FloatArray(0)
    private var spin = 0f

    override fun setup() {
        val md = min(width, height)
        val c = color(0)
        center.shader = RadialGradient(0f, 0f, md * 0.7f, intArrayOf(Col.alpha(c, 0.4f), Col.alpha(color(3), 0.12f), 0),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        center.xfermode = SCREEN
        glowP.strokeWidth = dp(7f)
        lineP.strokeWidth = dp(1.5f)
        val n = count(90)
        dustA = FloatArray(n) { rf(0f, 6.28f) }
        dustR = FloatArray(n) { rf(md * 0.1f, max(width, height) * 0.7f) }
        dustW = FloatArray(n) { rf(0.05f, 0.25f) * if (rnd.nextBoolean()) 1f else -1f }
        for (i in rot.indices) rot[i] = rf(0f, 6.28f)
    }

    override fun render(c: Canvas, t: Float, dt: Float) {
        val w = width; val h = height
        drawBackground(c)
        spin *= 0.97f
        val md = min(w, h)
        c.save()
        c.translate(w / 2 - px * w * 0.3f, h / 2)
        c.drawCircle(0f, 0f, md * 0.7f, center)
        for (i in dustA.indices) {
            dustA[i] += dustW[i] * dt * (1f + spin)
            val r = dustR[i]
            dotP.color = Col.alpha(Col.lighten(color(i), 0.4f), 0.35f + 0.35f * sin(t + i))
            c.drawCircle(cos(dustA[i]) * r, sin(dustA[i]) * r, dp(1.3f), dotP)
        }
        for (i in 0 until rings) {
            val dir = if (i % 2 == 0) 1f else -1f
            rot[i] += dt * (0.12f + 0.025f * i) * dir * (1f + spin * 3f)
            val r = md * (0.05f + i * 0.047f) * (1f + 0.05f * sin(t * 0.7f + i * 0.6f))
            val inner = 0.55f + 0.3f * sin(t * 0.4f + i * 0.8f)
            val n = sides[i]
            path.reset()
            for (k in 0 until n * 2) {
                val a = rot[i] + k * PI.toFloat() / n
                val rr = if (k % 2 == 0) r else r * inner
                val x = cos(a) * rr; val y = sin(a) * rr
                if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            val col = Col.lerp(color(i), color(i + 1), 0.5f + 0.5f * sin(t * 0.3f + i))
            glowP.color = Col.alpha(col, 0.16f)
            c.drawPath(path, glowP)
            lineP.color = Col.alpha(Col.lighten(col, 0.25f), 0.85f)
            c.drawPath(path, lineP)
            if (i % 2 == 1) {
                dotP.color = Col.lighten(col, 0.5f)
                for (k in 0 until n) {
                    val a = rot[i] + k * 2f * PI.toFloat() / n
                    c.drawCircle(cos(a) * r, sin(a) * r, dp(2.2f), dotP)
                }
            }
        }
        c.restore()
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        if (action == MotionEvent.ACTION_DOWN) spin = 2.5f
        else if (action == MotionEvent.ACTION_MOVE) spin = min(3f, spin + 0.08f)
    }
}
