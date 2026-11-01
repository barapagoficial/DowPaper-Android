package com.dowpaper.android.scenes

import android.content.Context
import android.graphics.*
import com.dowpaper.android.core.Palette
import com.dowpaper.android.core.Palettes
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Escena animada dibujada con Canvas. La misma clase se usa en el servicio de
 * fondo de pantalla y en las vistas previas de la app, garantizando que lo que
 * ves es exactamente lo que obtienes.
 */
abstract class Scene(protected val ctx: Context) {
    var width = 0f; private set
    var height = 0f; private set
    var palette: Palette = Palettes.all[0]; private set
    var speed = 1f
    var density = 1f; private set
    /** 0..1 desplazamiento horizontal entre pantallas de inicio. */
    var parallax = 0.5f
    var touchEnabled = true
    /** En modo overlay la escena no pinta fondo (se usa sobre fotos). */
    var overlay = false

    protected val dens = ctx.resources.displayMetrics.density
    protected fun dp(v: Float) = v * dens
    protected var time = 0f
    protected var ready = false; private set
    protected val rnd = java.util.Random(7)
    private val bgPaint = Paint()

    fun configure(p: Palette, speed: Float, density: Float) {
        palette = p
        this.speed = speed
        this.density = density
        if (ready) rebuild()
    }

    fun resize(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        if (!ready || w.toFloat() != width || h.toFloat() != height) {
            width = w.toFloat(); height = h.toFloat()
            rebuild()
        }
    }

    private fun rebuild() {
        rnd.setSeed(seed())
        bgPaint.shader = LinearGradient(0f, 0f, 0f, height, palette.bgTop, palette.bgBottom, Shader.TileMode.CLAMP)
        setup()
        ready = true
    }

    protected open fun seed(): Long = 7L
    protected abstract fun setup()
    protected abstract fun render(c: Canvas, t: Float, dt: Float)

    fun frame(c: Canvas, dtRaw: Float) {
        if (!ready) return
        val dt = dtRaw.coerceIn(0f, 0.1f) * speed
        time += dt
        render(c, time, dt)
    }

    protected fun drawBackground(c: Canvas) {
        if (!overlay) c.drawRect(0f, 0f, width, height, bgPaint)
    }

    /** action: MotionEvent.ACTION_DOWN / MOVE / UP */
    fun touch(x: Float, y: Float, action: Int) {
        if (touchEnabled && ready) onTouch(x, y, action)
    }

    protected open fun onTouch(x: Float, y: Float, action: Int) {}
    open fun release() {}

    protected fun rf(a: Float, b: Float) = a + rnd.nextFloat() * (b - a)
    protected fun count(base: Int) = max(1, (base * density).roundToInt())
    protected fun color(i: Int): Int {
        val n = palette.colors.size
        return palette.colors[((i % n) + n) % n]
    }

    /** -0.5..0.5 */
    protected val px: Float get() = parallax - 0.5f

    companion object {
        val SCREEN = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        val ADD = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }
}
