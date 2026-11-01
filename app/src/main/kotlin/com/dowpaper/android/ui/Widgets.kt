package com.dowpaper.android.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import com.dowpaper.android.core.Col
import com.dowpaper.android.core.Palette
import com.dowpaper.android.core.dp
import com.dowpaper.android.scenes.Scene
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Vista que anima una escena. Se pausa sola cuando no está en pantalla. */
class SceneView(ctx: Context) : View(ctx), Choreographer.FrameCallback {
    var scene: Scene? = null
        set(v) {
            if (field !== v) field?.release()
            field = v; lastDraw = 0L; invalidate()
        }
    var maxFps = 60
    var virtualWidth = 0f
    var interactive = false
    var paused = false
    var onTap: (() -> Unit)? = null
    private val rect = Rect()
    private var lastInv = 0L
    private var lastDraw = 0L
    private var attached = false

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attached = true
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onDetachedFromWindow() {
        attached = false
        Choreographer.getInstance().removeFrameCallback(this)
        super.onDetachedFromWindow()
    }

    override fun doFrame(now: Long) {
        if (!attached) return
        Choreographer.getInstance().postFrameCallback(this)
        if (paused || !appVisible || scene == null) return
        if (now - lastInv < 1_000_000_000L / maxFps * 0.85f) return
        if (!isShown || !getGlobalVisibleRect(rect)) return
        lastInv = now
        invalidate()
    }

    private fun k(): Float = if (virtualWidth > 0f && width > 0) width / virtualWidth else 1f

    override fun onDraw(c: Canvas) {
        val s = scene ?: return
        if (width == 0 || height == 0) return
        val now = System.nanoTime()
        val dt = if (lastDraw == 0L) 0.016f else min(0.1f, (now - lastDraw) / 1e9f)
        lastDraw = now
        val k = k()
        if (k != 1f) {
            c.save(); c.scale(k, k)
            s.resize(virtualWidth.roundToInt(), (height / k).roundToInt())
            s.frame(c, dt)
            c.restore()
        } else {
            s.resize(width, height)
            s.frame(c, dt)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (!interactive) return super.onTouchEvent(e)
        val k = k()
        scene?.touch(e.x / k, e.y / k, e.actionMasked)
        if (e.actionMasked == MotionEvent.ACTION_DOWN) parent?.requestDisallowInterceptTouchEvent(true)
        return true
    }

    companion object {
        @JvmField var appVisible = true
    }
}

enum class Ic { BACK, CHEVRON, GEAR, HEART, HEART_FILL, EYE, EYE_OFF, CHECK, IMAGE, VIDEO, SPARK, SHUFFLE, LAYERS, PLAY }

class IconView(ctx: Context, icon: Ic, tint: Int = Color.WHITE) : View(ctx) {
    var icon: Ic = icon
        set(v) { field = v; invalidate() }
    var tint: Int = tint
        set(v) { field = v; invalidate() }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val path = Path()
    private val rf = RectF()

    override fun onDraw(c: Canvas) {
        val s = min(width - paddingLeft - paddingRight, height - paddingTop - paddingBottom).toFloat()
        if (s <= 0) return
        c.save()
        c.translate(paddingLeft + (width - paddingLeft - paddingRight - s) / 2, paddingTop + (height - paddingTop - paddingBottom - s) / 2)
        val u = s / 24f
        c.scale(u, u)
        p.color = tint
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        path.reset()
        when (icon) {
            Ic.BACK -> { path.moveTo(15f, 5f); path.lineTo(8f, 12f); path.lineTo(15f, 19f); c.drawPath(path, p) }
            Ic.CHEVRON -> { path.moveTo(9f, 5f); path.lineTo(16f, 12f); path.lineTo(9f, 19f); c.drawPath(path, p) }
            Ic.CHECK -> { p.strokeWidth = 2.4f; path.moveTo(5f, 12.5f); path.lineTo(10f, 17.5f); path.lineTo(19f, 7f); c.drawPath(path, p) }
            Ic.HEART, Ic.HEART_FILL -> {
                path.moveTo(12f, 20.5f)
                path.cubicTo(5f, 15.5f, 2.5f, 12f, 2.5f, 8.5f)
                path.cubicTo(2.5f, 5.5f, 4.8f, 3.5f, 7.5f, 3.5f)
                path.cubicTo(9.5f, 3.5f, 11f, 4.6f, 12f, 6.2f)
                path.cubicTo(13f, 4.6f, 14.5f, 3.5f, 16.5f, 3.5f)
                path.cubicTo(19.2f, 3.5f, 21.5f, 5.5f, 21.5f, 8.5f)
                path.cubicTo(21.5f, 12f, 19f, 15.5f, 12f, 20.5f)
                path.close()
                if (icon == Ic.HEART_FILL) { p.style = Paint.Style.FILL; p.color = T.PINK }
                c.drawPath(path, p)
            }
            Ic.GEAR -> {
                c.drawCircle(12f, 12f, 3.2f, p)
                c.drawCircle(12f, 12f, 7f, p)
                p.strokeWidth = 2.6f
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    c.drawLine(12f + 7.2f * Math.cos(a).toFloat(), 12f + 7.2f * Math.sin(a).toFloat(),
                        12f + 9.6f * Math.cos(a).toFloat(), 12f + 9.6f * Math.sin(a).toFloat(), p)
                }
            }
            Ic.EYE, Ic.EYE_OFF -> {
                path.moveTo(2f, 12f); path.quadTo(12f, 2.5f, 22f, 12f); path.quadTo(12f, 21.5f, 2f, 12f); path.close()
                c.drawPath(path, p); c.drawCircle(12f, 12f, 3f, p)
                if (icon == Ic.EYE_OFF) c.drawLine(4f, 4f, 20f, 20f, p)
            }
            Ic.IMAGE -> {
                rf.set(3f, 5f, 21f, 19f); c.drawRoundRect(rf, 2.5f, 2.5f, p)
                c.drawCircle(15.5f, 9.5f, 1.6f, p)
                path.moveTo(4f, 17f); path.lineTo(9.5f, 11.5f); path.lineTo(13f, 15f); path.lineTo(15.5f, 12.5f); path.lineTo(20f, 17f)
                c.drawPath(path, p)
            }
            Ic.VIDEO -> {
                rf.set(3f, 6f, 16f, 18f); c.drawRoundRect(rf, 2.5f, 2.5f, p)
                path.moveTo(16f, 10.5f); path.lineTo(21f, 7.5f); path.lineTo(21f, 16.5f); path.lineTo(16f, 13.5f); c.drawPath(path, p)
            }
            Ic.PLAY -> { p.style = Paint.Style.FILL; path.moveTo(8f, 5f); path.lineTo(19f, 12f); path.lineTo(8f, 19f); path.close(); c.drawPath(path, p) }
            Ic.SPARK -> {
                p.style = Paint.Style.FILL
                path.moveTo(12f, 2f); path.lineTo(14f, 10f); path.lineTo(22f, 12f); path.lineTo(14f, 14f)
                path.lineTo(12f, 22f); path.lineTo(10f, 14f); path.lineTo(2f, 12f); path.lineTo(10f, 10f); path.close()
                c.drawPath(path, p)
            }
            Ic.SHUFFLE -> {
                path.moveTo(3f, 7f); path.lineTo(7f, 7f); path.cubicTo(12f, 7f, 12f, 17f, 17f, 17f); path.lineTo(21f, 17f)
                path.moveTo(3f, 17f); path.lineTo(7f, 17f); path.cubicTo(12f, 17f, 12f, 7f, 17f, 7f); path.lineTo(21f, 7f)
                path.moveTo(18f, 4f); path.lineTo(21f, 7f); path.lineTo(18f, 10f)
                path.moveTo(18f, 14f); path.lineTo(21f, 17f); path.lineTo(18f, 20f)
                c.drawPath(path, p)
            }
            Ic.LAYERS -> {
                path.moveTo(12f, 3f); path.lineTo(21f, 8f); path.lineTo(12f, 13f); path.lineTo(3f, 8f); path.close()
                path.moveTo(3f, 12.5f); path.lineTo(12f, 17.5f); path.lineTo(21f, 12.5f)
                path.moveTo(3f, 16.5f); path.lineTo(12f, 21.5f); path.lineTo(21f, 16.5f)
                c.drawPath(path, p)
            }
        }
        c.restore()
    }
}

/** Botón circular translúcido con icono. */
class CircleButton(ctx: Context, icon: Ic, sizeDp: Float = 46f) : FrameLayout(ctx) {
    val iconView = IconView(ctx, icon)

    init {
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0x40000000)
            setStroke(ctx.dp(1f).toInt().coerceAtLeast(1), 0x30FFFFFF)
        }
        val pad = (ctx.dp(sizeDp) * 0.26f).toInt()
        addView(iconView, flp(MATCH, MATCH, Gravity.CENTER, pad, pad, pad, pad))
        layoutParams = lp(ctx.dp(sizeDp).toInt(), ctx.dp(sizeDp).toInt())
        isClickable = true
        pressable(0.88f)
    }
}

/** Botón principal con gradiente fluido, brillo animado y estado de éxito. */
class GradientButton(ctx: Context, var text: String) : View(ctx) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shine = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = T.bold; textSize = ctx.dp(16.5f) }
    private val check = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = ctx.dp(2.6f); strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val rect = RectF()
    private val m = Matrix()
    private val sm = Matrix()
    private var grad: LinearGradient? = null
    private var shineSh: LinearGradient? = null
    private val t0 = System.nanoTime()
    private var t = 0f
    private var success = 0f
    private var successText = ""
    private val pad = ctx.dp(8f)
    private val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
        addUpdateListener { t = (System.nanoTime() - t0) / 1e9f; invalidate() }
    }

    init {
        isClickable = true
        pressable(0.96f)
        if (Build.VERSION.SDK_INT >= 28) fill.setShadowLayer(ctx.dp(18f), 0f, ctx.dp(6f), Col.alpha(T.A1, 0.6f))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (context.dp(58f) + pad * 2).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        grad = LinearGradient(0f, 0f, w.toFloat(), 0f, intArrayOf(T.A1, T.A2, T.A3, T.PINK), null, Shader.TileMode.MIRROR)
        fill.shader = grad
        shineSh = LinearGradient(0f, 0f, w * 0.25f, 0f, intArrayOf(0, 0x55FFFFFF, 0), null, Shader.TileMode.CLAMP)
        shine.shader = shineSh
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); anim.start() }
    override fun onDetachedFromWindow() { anim.cancel(); super.onDetachedFromWindow() }

    fun showSuccess(msg: String = "¡Listo!") {
        successText = msg
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 450; interpolator = OvershootInterpolator(2f)
            addUpdateListener { success = it.animatedValue as Float; invalidate() }
            start()
        }
        postDelayed({
            ValueAnimator.ofFloat(1f, 0f).apply {
                duration = 350
                addUpdateListener { success = it.animatedValue as Float; invalidate() }
                start()
            }
        }, 2200)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        rect.set(pad, pad, w - pad, h - pad)
        val r = rect.height() / 2
        m.setTranslate((t * 0.25f % 2f) * w, 0f); grad?.setLocalMatrix(m)
        c.drawRoundRect(rect, r, r, fill)
        val cyc = (t % 3.2f) / 1.2f
        if (cyc < 1f) {
            sm.setTranslate(-w * 0.3f + cyc * w * 1.4f, 0f); shineSh?.setLocalMatrix(sm)
            c.drawRoundRect(rect, r, r, shine)
        }
        val cy = h / 2 - (tp.descent() + tp.ascent()) / 2
        val s = success.coerceIn(0f, 1.2f)
        if (s < 0.98f) {
            tp.alpha = ((1f - s.coerceAtMost(1f)) * 255).toInt()
            c.drawText(text, w / 2, cy - s * context.dp(16f), tp)
        }
        if (s > 0.02f) {
            tp.alpha = (s.coerceAtMost(1f) * 255).toInt()
            val tw = tp.measureText(successText)
            val ox = w / 2 + context.dp(14f)
            c.drawText(successText, ox, cy + (1f - s) * context.dp(16f), tp)
            val ix = ox - tw / 2 - context.dp(26f); val iy = h / 2 + (1f - s) * context.dp(16f)
            check.alpha = tp.alpha
            val u = context.dp(1f)
            c.drawLine(ix, iy, ix + 5 * u, iy + 5 * u, check)
            c.drawLine(ix + 5 * u, iy + 5 * u, ix + 15 * u, iy - 6 * u, check)
        }
        tp.alpha = 255
    }
}

/** Botón secundario translúcido con icono y texto. */
class GhostButton(ctx: Context, text: String, icon: Ic) : FrameLayout(ctx) {
    val label: TextView = ctx.label(text, 15f, T.TEXT, T.medium)

    init {
        background = ctx.glass(18f, T.GLASS2, 0x30FFFFFF)
        val row = android.widget.LinearLayout(ctx).apply { gravity = Gravity.CENTER; orientation = android.widget.LinearLayout.HORIZONTAL }
        val s = ctx.dp(20f).toInt()
        row.addView(IconView(ctx, icon), android.widget.LinearLayout.LayoutParams(s, s).apply { rightMargin = ctx.dp(10f).toInt() })
        row.addView(label)
        addView(row, flp(MATCH, MATCH, Gravity.CENTER))
        isClickable = true
        pressable(0.96f)
    }
}

/** Slider con pista de gradiente y perilla luminosa. */
class GlassSlider(
    ctx: Context, private val title: String, private val minV: Float, private val maxV: Float,
    initial: Float, private val fmt: (Float) -> String
) : View(ctx) {
    var value = initial.coerceIn(minV, maxV); private set
    var onChange: ((Float) -> Unit)? = null
    var onCommit: ((Float) -> Unit)? = null
    private val tl = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = T.TEXT2; textSize = ctx.dp(14f); typeface = T.medium }
    private val tv = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = T.TEXT; textSize = ctx.dp(14f); typeface = T.bold; textAlign = Paint.Align.RIGHT }
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x26FFFFFF }
    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG)
    private val knob = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Col.alpha(T.A3, 0.25f) }
    private val rf = RectF()
    private var drag = 0f
    private var dragging = false
    private val kr = ctx.dp(11f)

    init { if (Build.VERSION.SDK_INT >= 28) knob.setShadowLayer(ctx.dp(6f), 0f, ctx.dp(2f), 0x66000000) }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), context.dp(64f).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        fillP.shader = LinearGradient(kr, 0f, w - kr, 0f, intArrayOf(T.A1, T.A2, T.A3), null, Shader.TileMode.CLAMP)
    }

    private val trackY get() = context.dp(44f)

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        c.drawText(title, 0f, context.dp(16f), tl)
        c.drawText(fmt(value), w, context.dp(16f), tv)
        val th = context.dp(6f)
        rf.set(kr, trackY - th / 2, w - kr, trackY + th / 2)
        c.drawRoundRect(rf, th, th, track)
        val f = (value - minV) / (maxV - minV)
        val kx = kr + (w - 2 * kr) * f
        rf.right = kx
        c.drawRoundRect(rf, th, th, fillP)
        if (drag > 0.01f) c.drawCircle(kx, trackY, kr * (1f + drag * 0.9f), halo)
        c.drawCircle(kx, trackY, kr * (1f + drag * 0.12f), knob)
    }

    private fun setFromX(x: Float) {
        val f = ((x - kr) / (width - 2 * kr)).coerceIn(0f, 1f)
        val v = minV + (maxV - minV) * f
        if (v != value) { value = v; onChange?.invoke(v); invalidate() }
    }

    private fun animDrag(to: Float) {
        ValueAnimator.ofFloat(drag, to).apply {
            duration = 180; addUpdateListener { drag = it.animatedValue as Float; invalidate() }; start()
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (e.y < context.dp(24f)) return false
                dragging = true; parent?.requestDisallowInterceptTouchEvent(true); animDrag(1f); setFromX(e.x)
            }
            MotionEvent.ACTION_MOVE -> if (dragging) setFromX(e.x)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) { dragging = false; animDrag(0f); onCommit?.invoke(value) }
            }
        }
        return true
    }
}

/** Interruptor animado. */
class ToggleSwitch(ctx: Context, checked: Boolean) : View(ctx) {
    var checked = checked; private set
    var onChange: ((Boolean) -> Unit)? = null
    private var frac = if (checked) 1f else 0f
    private val off = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x2EFFFFFF }
    private val on = Paint(Paint.ANTI_ALIAS_FLAG)
    private val knob = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val rf = RectF()

    init {
        isClickable = true
        setOnClickListener { set(!this.checked, true) }
    }

    fun set(v: Boolean, notify: Boolean) {
        checked = v
        ValueAnimator.ofFloat(frac, if (v) 1f else 0f).apply {
            duration = 260; interpolator = OvershootInterpolator(1.6f)
            addUpdateListener { frac = it.animatedValue as Float; invalidate() }
            start()
        }
        if (notify) onChange?.invoke(v)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(context.dp(52f).toInt(), context.dp(30f).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        on.shader = LinearGradient(0f, 0f, w.toFloat(), 0f, T.A1, T.A3, Shader.TileMode.CLAMP)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        rf.set(0f, 0f, w, h)
        c.drawRoundRect(rf, h / 2, h / 2, off)
        on.alpha = (frac.coerceIn(0f, 1f) * 255).toInt()
        c.drawRoundRect(rf, h / 2, h / 2, on)
        val r = h / 2 - context.dp(4f)
        val x = h / 2 + (w - h) * frac
        c.drawCircle(x, h / 2, r, knob)
    }
}

/** Control segmentado con indicador deslizante. */
class Segmented(ctx: Context, private val options: List<String>, selected: Int) : View(ctx) {
    var selected = selected; private set
    var onSelect: ((Int) -> Unit)? = null
    private var pos = selected.toFloat()
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x14FFFFFF }
    private val ind = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; textSize = ctx.dp(13.5f) }
    private val rf = RectF()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), context.dp(44f).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        ind.shader = LinearGradient(0f, 0f, w.toFloat(), 0f, intArrayOf(T.A1, T.A2, T.A3), null, Shader.TileMode.CLAMP)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        rf.set(0f, 0f, w, h); c.drawRoundRect(rf, h / 2, h / 2, bg)
        val sw = w / options.size
        val inset = context.dp(4f)
        rf.set(pos * sw + inset, inset, (pos + 1) * sw - inset, h - inset)
        c.drawRoundRect(rf, h / 2, h / 2, ind)
        val cy = h / 2 - (tp.descent() + tp.ascent()) / 2
        for ((i, o) in options.withIndex()) {
            val sel = 1f - min(1f, kotlin.math.abs(pos - i))
            tp.color = Col.lerp(T.TEXT2, Color.WHITE, sel)
            tp.typeface = if (sel > 0.5f) T.bold else T.medium
            c.drawText(o, sw * i + sw / 2, cy, tp)
        }
    }

    fun select(i: Int, notify: Boolean) {
        selected = i
        ValueAnimator.ofFloat(pos, i.toFloat()).apply {
            duration = 300; interpolator = DecelerateInterpolator(2f)
            addUpdateListener { pos = it.animatedValue as Float; invalidate() }
            start()
        }
        if (notify) onSelect?.invoke(i)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_UP) {
            val i = (e.x / (width.toFloat() / options.size)).toInt().coerceIn(0, options.size - 1)
            if (i != selected) select(i, true)
        }
        return true
    }
}

/** Chip de categoría con transición de selección. */
class ChipView(ctx: Context, val text: String, selected: Boolean) : View(ctx) {
    var selectedChip = selected; private set
    private var frac = if (selected) 1f else 0f
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = ctx.dp(14f); typeface = T.medium; textAlign = Paint.Align.CENTER }
    private val glassP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x14FFFFFF }
    private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x24FFFFFF; style = Paint.Style.STROKE; strokeWidth = ctx.dp(1f) }
    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rf = RectF()

    init { isClickable = true; pressable(0.92f) }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension((tp.measureText(text) + context.dp(36f)).toInt(), context.dp(38f).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        fillP.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), T.A1, T.A3, Shader.TileMode.CLAMP)
    }

    fun setSel(v: Boolean) {
        if (v == selectedChip) return
        selectedChip = v
        ValueAnimator.ofFloat(frac, if (v) 1f else 0f).apply {
            duration = 250; addUpdateListener { frac = it.animatedValue as Float; invalidate() }; start()
        }
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val sw = strokeP.strokeWidth / 2
        rf.set(sw, sw, w - sw, h - sw)
        c.drawRoundRect(rf, h / 2, h / 2, glassP)
        c.drawRoundRect(rf, h / 2, h / 2, strokeP)
        fillP.alpha = (frac * 255).toInt()
        c.drawRoundRect(rf, h / 2, h / 2, fillP)
        tp.color = Col.lerp(T.TEXT2, Color.WHITE, frac)
        tp.typeface = if (frac > 0.5f) T.bold else T.medium
        c.drawText(text, w / 2, h / 2 - (tp.descent() + tp.ascent()) / 2, tp)
    }
}

/** Muestra circular de una paleta. */
class PaletteSwatch(ctx: Context, val palette: Palette, selected: Boolean) : View(ctx) {
    private var frac = if (selected) 1f else 0f
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.WHITE; strokeWidth = ctx.dp(2.2f) }
    private val check = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.WHITE; strokeWidth = ctx.dp(2.4f); strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val bgP = Paint(Paint.ANTI_ALIAS_FLAG)

    init { isClickable = true; pressable(0.85f) }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val s = context.dp(54f).toInt(); setMeasuredDimension(s, s)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val cs = palette.colors
        fill.shader = SweepGradient(w / 2f, h / 2f, intArrayOf(cs[0], cs[1], cs[2], cs[3], cs[0]), null)
        bgP.color = palette.bgBottom
    }

    fun setSel(v: Boolean) {
        ValueAnimator.ofFloat(frac, if (v) 1f else 0f).apply {
            duration = 280; interpolator = OvershootInterpolator(2f)
            addUpdateListener { frac = it.animatedValue as Float; invalidate() }; start()
        }
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f; val cy = height / 2f
        val r = context.dp(17f) * (1f + 0.08f * frac)
        c.drawCircle(cx, cy, r + context.dp(1f), bgP)
        c.drawCircle(cx, cy, r, fill)
        if (frac > 0.01f) {
            ring.alpha = (frac.coerceIn(0f, 1f) * 255).toInt()
            c.drawCircle(cx, cy, r + context.dp(5f), ring)
            check.alpha = ring.alpha
            val u = context.dp(1f)
            c.drawLine(cx - 6 * u, cy, cx - 2 * u, cy + 4 * u, check)
            c.drawLine(cx - 2 * u, cy + 4 * u, cx + 6 * u, cy - 5 * u, check)
        }
    }
}

/** Texto con gradiente animado. */
class GradientText(ctx: Context) : TextView(ctx) {
    private val m = Matrix()
    private var sh: LinearGradient? = null
    private val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 7000; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
        addUpdateListener {
            m.setTranslate((it.animatedValue as Float) * width * 2f, 0f)
            sh?.setLocalMatrix(m); invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        sh = LinearGradient(0f, 0f, w.toFloat(), 0f, intArrayOf(Color.WHITE, 0xFFC9B8FF.toInt(), 0xFF7FE9FF.toInt(), 0xFFFFB8DC.toInt()), null, Shader.TileMode.MIRROR)
        paint.shader = sh
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); anim.start() }
    override fun onDetachedFromWindow() { anim.cancel(); super.onDetachedFromWindow() }
}

/** Rejilla de dos columnas con tarjetas verticales. */
class TwoColumnGrid(ctx: Context, private val gap: Int, private val ratio: Float = 1.62f) : ViewGroup(ctx) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val cw = (w - gap) / 2
        val ch = (cw * ratio).toInt()
        var n = 0
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.visibility == GONE) continue
            c.measure(MeasureSpec.makeMeasureSpec(cw, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(ch, MeasureSpec.EXACTLY))
            n++
        }
        val rows = (n + 1) / 2
        setMeasuredDimension(w, max(0, rows * ch + (rows - 1).coerceAtLeast(0) * gap))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var idx = 0
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            if (c.visibility == GONE) continue
            val col = idx % 2; val row = idx / 2
            val x = col * (c.measuredWidth + gap)
            val y = row * (c.measuredHeight + gap)
            c.layout(x, y, x + c.measuredWidth, y + c.measuredHeight)
            idx++
        }
    }
}

/** Vista previa de video en bucle, silenciado y con recorte centrado. */
class VideoPreview(ctx: Context) : TextureView(ctx), TextureView.SurfaceTextureListener {
    var uri: Uri? = null
        set(v) { field = v; restart() }
    private var mp: MediaPlayer? = null
    private var surface: Surface? = null
    private var vw = 0; private var vh = 0
    private val mx = Matrix()

    init { surfaceTextureListener = this }

    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) { surface = Surface(st); restart() }
    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) { fit() }
    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean { release(); surface?.release(); surface = null; return true }
    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}

    private fun restart() {
        release()
        val s = surface ?: return
        val u = uri ?: return
        try {
            mp = MediaPlayer().apply {
                setDataSource(context, u)
                setSurface(s)
                isLooping = true
                setVolume(0f, 0f)
                setOnVideoSizeChangedListener { _, w, h -> vw = w; vh = h; fit() }
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        } catch (e: Exception) { release() }
    }

    private fun fit() {
        if (vw == 0 || vh == 0 || width == 0) return
        val s = max(width.toFloat() / vw, height.toFloat() / vh)
        mx.setScale(vw * s / width, vh * s / height, width / 2f, height / 2f)
        setTransform(mx)
    }

    fun pause() { try { mp?.pause() } catch (e: Exception) {} }
    fun resume() { try { mp?.start() } catch (e: Exception) {} }

    fun release() {
        mp?.let { try { it.stop() } catch (e: Exception) {}; try { it.release() } catch (e: Exception) {} }
        mp = null
    }
}
