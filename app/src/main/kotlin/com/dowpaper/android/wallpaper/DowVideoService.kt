package com.dowpaper.android.wallpaper

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.dowpaper.android.core.Prefs

/**
 * Fondo de video. Es un servicio aparte porque un Surface que ya fue dibujado
 * con Canvas no puede ser reutilizado por MediaPlayer (y viceversa).
 */
class DowVideoService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val prefs = Prefs(this@DowVideoService)
        private val handler = Handler(Looper.getMainLooper())
        private var player: MediaPlayer? = null
        private var visible = false
        private var hasSurface = false
        private var loadedUri: String? = null
        private var failed = false

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            prefs.sp.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onDestroy() {
            prefs.sp.unregisterOnSharedPreferenceChangeListener(this)
            release()
            handler.removeCallbacksAndMessages(null)
            super.onDestroy()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            hasSurface = true
            start()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            hasSurface = false
            release()
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            val p = player
            if (p == null) { if (v) start(); return }
            try { if (v) p.start() else p.pause() } catch (e: Exception) {}
        }

        override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
            if (key == "video_uri") handler.post { if (prefs.videoUri != loadedUri) { failed = false; release(); start() } }
        }

        private fun start() {
            if (!hasSurface || player != null || failed) return
            val uri = prefs.videoUri ?: run { drawMessage("Elige un video en la app DowPaper"); return }
            try {
                val mp = MediaPlayer()
                mp.setDataSource(applicationContext, Uri.parse(uri))
                mp.setSurface(surfaceHolder.surface)
                mp.isLooping = true
                mp.setVolume(0f, 0f)
                mp.setOnPreparedListener {
                    try { it.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING) } catch (e: Exception) {}
                    if (visible || isPreview) it.start()
                }
                mp.setOnErrorListener { _, _, _ ->
                    handler.post { failed = true; release(); drawMessage("No se pudo reproducir el video") }
                    true
                }
                mp.prepareAsync()
                player = mp
                loadedUri = uri
            } catch (e: Exception) {
                failed = true
                release()
                drawMessage("No se pudo abrir el video")
            }
        }

        private fun release() {
            player?.let { try { it.stop() } catch (e: Exception) {}; try { it.release() } catch (e: Exception) {} }
            player = null
        }

        /** Solo se usa si no hay video reproducible; después de esto el Surface queda para Canvas. */
        private fun drawMessage(msg: String) {
            if (!hasSurface) return
            val h = surfaceHolder
            val c = try { h.lockCanvas() } catch (e: Exception) { null } ?: return
            try {
                c.drawColor(0xFF07070D.toInt())
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE; textAlign = Paint.Align.CENTER
                    textSize = 18f * resources.displayMetrics.density
                }
                c.drawText(msg, c.width / 2f, c.height / 2f, p)
            } finally {
                try { h.unlockCanvasAndPost(c) } catch (e: Exception) {}
            }
        }
    }
}
