package com.dowpaper.android.scenes

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import com.dowpaper.android.core.Col
import kotlin.math.*

class WavesScene(ctx: Context) : Scene(ctx) {
    private val layers = 6
    private val seg = 48
    private val sky = Paint()
    private val sunGlow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sunDisk = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sunGlowM = Matrix()
    private var sunSh: RadialGradient? = null
    private val layerP = Array(layers) { Paint(Paint.ANTI_ALIAS_FLAG) }
    private val shimmer = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val star = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val path = Path()
    private var stars = FloatArray(0)
    private var rx = 0f; private var rAge = 99f

    override fun setup() {
        val w = width; val h = height
        val horizonCol = Col.lerp(color(0), palette.bgBottom, 0.35f)
        sky.shader = LinearGradient(0f, 0f, 0f, h * 0.58f, intArrayOf(palette.bgTop, palette.bgBottom, horizonCol),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        val sc = Col.lighten(color(1), 0.2f)
        val sh = RadialGradient(0f, 0f, w * 0.6f, intArrayOf(Col.alpha(sc, 0.55f), Col.alpha(sc, 0.15f), Col.alpha(sc, 0f)),
            floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP)
        sunSh = sh; sunGlow.shader = sh
        sunDisk.color = Col.lighten(color(2), 0.35f)
        val far = Col.lerp(horizonCol, color(0), 0.3f)
        val nearC = Col.darken(palette.bgTop, 0.5f)
        for (i in 0 until layers) {
            val k = i / (layers - 1f)
            val base = Col.lerp(far, nearC, k)
            val y0 = h * (0.52f + 0.075f * i)
            layerP[i].shader = LinearGradient(0f, y0 - h * 0.03f, 0f, h, Col.lighten(base, 0.08f), Col.darken(base, 0.45f), Shader.TileMode.CLAMP)
        }
        shimmer.color = Col.lighten(color(2), 0.5f)
        shimmer.strokeWidth = dp(2f)
        val n = count(60)
        stars = FloatArray(n * 3)
        for (i in 0 until n) { stars[i * 3] = rf(0f, w); stars[i * 3 + 1] = rf(0f, h * 0.4f); stars[i * 3 + 2] = rf(0f, 6.28f) }
    }

    override fun render(c: Canvas, t: Float, dt: Float) {
        val w = width; val h = height
        rAge += dt
        if (!overlay) c.drawRect(0f, 0f, w, h * 0.62f, sky)
        for (i in 0 until stars.size / 3) {
            val y = stars[i * 3 + 1]
            star.alpha = ((0.3f + 0.7f * (0.5f + 0.5f * sin(t * 1.5f + stars[i * 3 + 2]))) * 200 * (1f - y / (h * 0.4f))).toInt().coerceIn(0, 255)
            c.drawCircle(stars[i * 3], y, dp(1f), star)
        }
        val sunX = w * 0.5f - px * w * 0.05f
        val sunY = h * (0.42f + 0.015f * sin(t * 0.08f))
        sunGlowM.setTranslate(sunX, sunY); sunSh?.setLocalMatrix(sunGlowM)
        c.drawCircle(sunX, sunY, w * 0.6f, sunGlow)
        c.drawCircle(sunX, sunY, w * 0.11f, sunDisk)
        for (i in 0 until layers) {
            val base = h * (0.52f + 0.075f * i)
            val amp = h * (0.008f + 0.007f * i)
            val fr = 1.4f + 0.45f * i
            val spd = 0.35f + 0.18f * i
            val shift = px * w * 0.05f * (i + 1)
            path.reset()
            path.moveTo(-w * 0.1f, h)
            for (k in 0..seg) {
                val x = -w * 0.1f + w * 1.2f * k / seg
                val nx = (x + shift) / w * 6.2832f
                var y = base + amp * sin(nx * fr + t * spd + i) + amp * 0.55f * sin(nx * fr * 2.3f - t * spd * 1.4f + i * 2f)
                if (rAge < 5f) {
                    val d = abs(x - rx) / w
                    y += h * 0.03f * exp(-d * 5f) * exp(-rAge * 0.7f) * sin(d * 38f - rAge * 7f) * (i + 1f) / layers
                }
                path.lineTo(x, y)
            }
            path.lineTo(w * 1.1f, h)
            path.close()
            c.drawPath(path, layerP[i])
            if (i == 0 || i == 1) {
                for (s in 0 until 9) {
                    val yy = base + dp(10f) + s * dp(9f) * (i + 1)
                    val wl = w * 0.08f * (1f - s / 10f) * (0.6f + 0.4f * sin(t * 3f + s * 1.3f + i))
                    shimmer.alpha = ((0.25f + 0.35f * (0.5f + 0.5f * sin(t * 2.4f + s))) * 255 * (1f - s / 9f)).toInt()
                    c.drawLine(sunX - wl, yy, sunX + wl, yy, shimmer)
                }
            }
        }
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        if (action == MotionEvent.ACTION_DOWN) { rx = x; rAge = 0f }
    }
}
