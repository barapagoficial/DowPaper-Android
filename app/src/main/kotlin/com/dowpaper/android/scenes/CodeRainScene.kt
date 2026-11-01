package com.dowpaper.android.scenes

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import com.dowpaper.android.core.Col
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

class CodeRainScene(ctx: Context) : Scene(ctx) {
    private val glyphs = "ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝ0123456789ABCDEFXYZ<>+*=".toCharArray()
    private var cs = 0f
    private var cols = 0; private var rows = 0
    private var head = FloatArray(0); private var spd = FloatArray(0); private var len = IntArray(0)
    private var chars = Array(0) { CharArray(0) }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.MONOSPACE; textAlign = Paint.Align.CENTER }
    private val glow = Paint()

    override fun setup() {
        cs = dp(17f) / sqrt(density.coerceIn(0.3f, 2.5f))
        cols = (width / cs).toInt() + 1
        rows = (height / cs).toInt() + 2
        head = FloatArray(cols) { rf(0f, rows * 1.3f) }
        spd = FloatArray(cols) { rf(5f, 15f) }
        len = IntArray(cols) { rf(8f, 30f).toInt() }
        chars = Array(cols) { CharArray(rows) { glyphs[rnd.nextInt(glyphs.size)] } }
        p.textSize = cs * 0.92f
        glow.shader = LinearGradient(0f, height * 0.5f, 0f, height, 0, Col.alpha(color(0), 0.16f), Shader.TileMode.CLAMP)
    }

    override fun render(c: Canvas, t: Float, dt: Float) {
        val w = width; val h = height
        drawBackground(c)
        if (!overlay) c.drawRect(0f, 0f, w, h, glow)
        val headCol = Col.lighten(color(0), 0.82f)
        val shift = -px * w * 0.06f
        for (col in 0 until cols) {
            head[col] += spd[col] * dt
            if (head[col] - len[col] > rows) {
                head[col] = -rf(0f, rows * 0.25f); spd[col] = rf(6f, 16f); len[col] = rf(10f, 32f).toInt()
            }
            if (rnd.nextFloat() < 0.12f) chars[col][rnd.nextInt(rows)] = glyphs[rnd.nextInt(glyphs.size)]
            val x = col * cs + cs / 2 + shift
            val hr = floor(head[col]).toInt()
            val base = if (col % 3 == 0) color(1) else color(0)
            val l = len[col]
            for (k in 0 until l) {
                val r = hr - k
                if (r < 0 || r >= rows) continue
                val a = 1f - k.toFloat() / l
                p.color = if (k == 0) headCol else Col.alpha(Col.lighten(base, 0.15f), 0.15f + a * 0.85f)
                c.drawText(chars[col], r, 1, x, r * cs + cs * 0.85f, p)
            }
        }
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        if (action == MotionEvent.ACTION_UP) return
        val cc = (x / cs).toInt()
        for (col in 0 until cols) {
            if (abs(col - cc) <= 2) {
                head[col] = y / cs + rf(0f, 3f)
                spd[col] = rf(18f, 28f)
            }
        }
    }
}
