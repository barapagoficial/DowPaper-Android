package com.dowpaper.android.scenes

import android.content.Context
import android.graphics.*
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.dowpaper.android.core.Col
import kotlin.math.max
import kotlin.math.sin

/**
 * Muestra la foto del usuario (o un fotograma del video) con zoom lento estilo
 * Ken Burns, parallax y un efecto de partículas opcional encima.
 */
class PhotoScene(ctx: Context) : Scene(ctx) {
    var uri: String? = null
    var effect: String = "bokeh"
    var dim: Float = 0.2f
    var videoFrame = false

    private var bmp: Bitmap? = null
    private var fx: Scene? = null
    private val bp = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val m = Matrix()
    private val dimP = Paint()
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER }
    private val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val iconP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private var placeholder: Scene? = null
    private val shade = Paint().apply { color = Col.alpha(Color.BLACK, 0.25f) }
    private val ip = Path()

    override fun setup() {
        val w = width.toInt(); val h = height.toInt()
        bmp = BitmapCache.get(ctx, uri, videoFrame, w, h)
        fx = when (effect) {
            "bokeh" -> BokehScene(ctx); "snow" -> SnowScene(ctx); "rain" -> RainScene(ctx)
            "fireflies" -> FirefliesScene(ctx); "stars" -> CosmosScene(ctx); else -> null
        }?.also { s ->
            s.overlay = true
            s.configure(palette, 1f, density)
            s.resize(w, h)
        }
        if (bmp == null) {
            placeholder = FluidScene(ctx).also { it.configure(palette, 0.6f, 1f); it.resize(w, h) }
        } else placeholder = null
        dimP.color = Col.alpha(Color.BLACK, dim)
        textP.textSize = dp(22f); textP.isFakeBoldText = true
        sub.textSize = dp(14f); sub.color = Col.alpha(Color.WHITE, 0.7f)
        iconP.strokeWidth = dp(2.5f); iconP.color = Color.WHITE
    }

    override fun render(c: Canvas, t: Float, dt: Float) {
        val w = width; val h = height
        val b = bmp
        if (b == null) {
            placeholder?.frame(c, dt)
            c.drawRect(0f, 0f, w, h, shade)
            val cy = h * 0.45f
            val s = dp(34f)
            if (videoFrame) {
                c.drawRoundRect(w / 2 - s, cy - s * 0.7f, w / 2 + s, cy + s * 0.7f, dp(12f), dp(12f), iconP)
                ip.reset(); ip.apply { moveTo(w / 2 - s * 0.25f, cy - s * 0.35f); lineTo(w / 2 + s * 0.4f, cy); lineTo(w / 2 - s * 0.25f, cy + s * 0.35f); close() }
                c.drawPath(ip, iconP)
            } else {
                c.drawRoundRect(w / 2 - s, cy - s * 0.75f, w / 2 + s, cy + s * 0.75f, dp(12f), dp(12f), iconP)
                c.drawCircle(w / 2 + s * 0.4f, cy - s * 0.3f, s * 0.15f, iconP)
                ip.reset(); ip.apply { moveTo(w / 2 - s * 0.8f, cy + s * 0.55f); lineTo(w / 2 - s * 0.2f, cy - s * 0.1f); lineTo(w / 2 + s * 0.15f, cy + s * 0.25f); lineTo(w / 2 + s * 0.4f, cy + s * 0.05f); lineTo(w / 2 + s * 0.8f, cy + s * 0.55f) }
                c.drawPath(ip, iconP)
            }
            c.drawText(if (videoFrame) "Elige un video" else "Elige una foto", w / 2, cy + s + dp(40f), textP)
            c.drawText("Ábrelo desde la app DowPaper", w / 2, cy + s + dp(66f), sub)
        } else {
            val zoom = 1.1f + 0.05f * sin(t * 0.07f)
            val sc = max(w / b.width, h / b.height) * zoom
            val bw = b.width * sc; val bh = b.height * sc
            val extra = bw - w
            val dx = -extra / 2 - px * extra * 0.9f + sin(t * 0.05f) * extra * 0.05f
            val dy = (h - bh) / 2 + sin(t * 0.04f + 1f) * (bh - h) * 0.25f
            m.setScale(sc, sc); m.postTranslate(dx, dy)
            c.drawBitmap(b, m, bp)
            if (dim > 0.01f) c.drawRect(0f, 0f, w, h, dimP)
        }
        fx?.let { it.parallax = parallax; it.frame(c, dt) }
    }

    override fun onTouch(x: Float, y: Float, action: Int) {
        fx?.touch(x, y, action)
        placeholder?.touch(x, y, action)
    }
}

object BitmapCache {
    private val map = object : LinkedHashMap<String, Bitmap?>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap?>?): Boolean = size > 3
    }

    @Synchronized
    fun invalidate() { map.clear() }

    @Synchronized
    fun get(ctx: Context, uri: String?, video: Boolean, w: Int, h: Int): Bitmap? {
        if (uri == null) return null
        val target = max(w, h)
        val k = "$uri|$video|${target / 200}"
        if (map.containsKey(k)) return map[k]
        val b = try { if (video) loadVideoFrame(ctx, uri, target) else loadImage(ctx, uri, target) } catch (e: Throwable) { null }
        map[k] = b
        return b
    }

    private fun loadImage(ctx: Context, uri: String, target: Int): Bitmap? {
        val u = Uri.parse(uri)
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, o) }
        if (o.outWidth <= 0) return null
        var sample = 1
        while (max(o.outWidth, o.outHeight) / (sample * 2) >= target * 1.2f) sample *= 2
        val o2 = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = ctx.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, o2) } ?: return null
        return rotateIfNeeded(ctx, u, raw)
    }

    private fun rotateIfNeeded(ctx: Context, u: Uri, b: Bitmap): Bitmap {
        val deg = try {
            ctx.contentResolver.openInputStream(u)?.use {
                when (android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) {
                    6 -> 90f; 3 -> 180f; 8 -> 270f; else -> 0f
                }
            } ?: 0f
        } catch (e: Throwable) { 0f }
        if (deg == 0f) return b
        val mm = Matrix().apply { postRotate(deg) }
        return Bitmap.createBitmap(b, 0, 0, b.width, b.height, mm, true)
    }

    private fun loadVideoFrame(ctx: Context, uri: String, target: Int): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(ctx, Uri.parse(uri))
            val f = r.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
            val s = target.toFloat() / max(f.width, f.height)
            if (s < 1f) Bitmap.createScaledBitmap(f, (f.width * s).toInt(), (f.height * s).toInt(), true) else f
        } finally {
            try { r.release() } catch (e: Throwable) {}
        }
    }
}
