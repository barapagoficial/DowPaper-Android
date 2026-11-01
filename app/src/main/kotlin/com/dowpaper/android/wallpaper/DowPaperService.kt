package com.dowpaper.android.wallpaper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.service.wallpaper.WallpaperService
import android.view.*
import com.dowpaper.android.core.Catalog
import com.dowpaper.android.core.Kind
import com.dowpaper.android.core.Prefs
import com.dowpaper.android.scenes.Scene

class DowPaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = DowEngine()

    inner class DowEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener, Choreographer.FrameCallback {
        private val prefs = Prefs(this@DowPaperService)
        private val handler = Handler(Looper.getMainLooper())
        private val choreographer = Choreographer.getInstance()
        private val power = getSystemService(Context.POWER_SERVICE) as PowerManager
        private var scene: Scene? = null
        private var w = 0; private var h = 0
        private var visible = false
        private var running = false
        private var lastDraw = 0L
        private var useHw = true
        private var xOffset = 0.5f
        private var gestures: GestureDetector? = null
        private val reloadRunnable = Runnable { reload() }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            setOffsetNotificationsEnabled(true)
            prefs.sp.registerOnSharedPreferenceChangeListener(this)
            gestures = GestureDetector(this@DowPaperService, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (prefs.doubleTap && !isPreview) nextWallpaper(); return true
                }
            })
        }

        override fun onDestroy() {
            super.onDestroy()
            prefs.sp.unregisterOnSharedPreferenceChangeListener(this)
            stopDrawing()
            scene?.release(); scene = null
            handler.removeCallbacksAndMessages(null)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            val changed = width != w || height != h
            w = width; h = height
            if (scene == null || changed) reload() else drawOnce()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            stopDrawing()
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            if (v) {
                if (maybeRotate()) return
                startDrawing()
            } else {
                stopDrawing()
            }
        }

        override fun onOffsetsChanged(xOff: Float, yOff: Float, xStep: Float, yStep: Float, xPix: Int, yPix: Int) {
            xOffset = xOff
            applyParallax()
        }

        override fun onTouchEvent(event: MotionEvent) {
            gestures?.onTouchEvent(event)
            val s = scene ?: return
            if (!prefs.touch) return
            s.touch(event.x, event.y, event.actionMasked)
        }

        override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
            if (key == null || !relevant(key)) return
            handler.removeCallbacks(reloadRunnable)
            handler.postDelayed(reloadRunnable, 80)
        }

        private fun relevant(key: String): Boolean {
            val a = prefs.active
            return when {
                key == Prefs.K_ACTIVE || key == "touch" || key == "parallax" -> true
                key.startsWith("pal_") || key.startsWith("spd_") || key.startsWith("den_") -> key.endsWith("_$a")
                key.startsWith("photo") -> a == "photo"
                else -> false
            }
        }

        private fun applyParallax() {
            scene?.parallax = if (prefs.parallax && !isPreview) xOffset else 0.5f
        }

        private fun reload() {
            if (w <= 0 || h <= 0) return
            var def = Catalog.byId(prefs.active)
            if (def.kind == Kind.VIDEO) def = Catalog.all[0]
            stopDrawing()
            scene?.release(); scene = null
            val s = Catalog.create(this@DowPaperService, def, prefs)
            s.resize(w, h)
            scene = s
            applyParallax()
            if (visible) startDrawing() else drawOnce()
        }

        private fun maybeRotate(): Boolean {
            if (isPreview) return false
            val mins = prefs.rotateMinutes
            if (mins <= 0) return false
            val now = System.currentTimeMillis()
            val last = prefs.lastRotate
            if (last == 0L) { prefs.lastRotate = now; return false }
            if (now - last < mins * 60_000L) return false
            prefs.lastRotate = now
            nextWallpaper()
            return true
        }

        private fun nextWallpaper() {
            val favs = prefs.favorites
            var pool = Catalog.all.filter { it.kind == Kind.SCENE }.map { it.id }
            if (prefs.rotateFavs) {
                val fp = Catalog.all.filter { favs.contains(it.id) && it.kind != Kind.VIDEO && (it.kind != Kind.PHOTO || prefs.photoUri != null) }.map { it.id }
                if (fp.isNotEmpty()) pool = fp
            }
            if (pool.isEmpty()) return
            val cur = pool.indexOf(prefs.active)
            prefs.active = pool[(cur + 1) % pool.size]
        }

        private fun startDrawing() {
            if (running || scene == null || !visible) return
            running = true
            lastDraw = 0L
            choreographer.postFrameCallback(this)
        }

        private fun stopDrawing() {
            running = false
            choreographer.removeFrameCallback(this)
        }

        private fun targetFps(): Int {
            var f = prefs.fps
            if (prefs.saver && power.isPowerSaveMode) f = minOf(f, 20)
            return f
        }

        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            choreographer.postFrameCallback(this)
            val interval = 1_000_000_000L / targetFps()
            if (lastDraw != 0L && frameTimeNanos - lastDraw < interval * 0.85f) return
            val dt = if (lastDraw == 0L) 0.016f else (frameTimeNanos - lastDraw) / 1e9f
            lastDraw = frameTimeNanos
            draw(dt)
        }

        private fun drawOnce() { draw(0f) }

        private fun draw(dt: Float) {
            val s = scene ?: return
            val holder = surfaceHolder
            var c: Canvas? = null
            try {
                c = if (useHw) {
                    try { holder.lockHardwareCanvas() } catch (e: Exception) { useHw = false; holder.lockCanvas() }
                } else holder.lockCanvas()
                if (c != null) s.frame(c, dt)
            } catch (e: Exception) {
            } finally {
                if (c != null) try { holder.unlockCanvasAndPost(c) } catch (e: Exception) {}
            }
        }
    }
}
