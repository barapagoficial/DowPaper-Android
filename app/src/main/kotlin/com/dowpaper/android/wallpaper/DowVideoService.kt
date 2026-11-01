package com.dowpaper.android.wallpaper

import android.content.SharedPreferences
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.dowpaper.android.core.Prefs

/**
 * Fondo de video. Es un servicio aparte porque un Surface que ya fue dibujado
 * con Canvas no puede ser reutilizado por MediaPlayer. Por la misma razón aquí
 * NUNCA se dibuja con Canvas: si algo falla se reintenta, sin bloquear el Surface.
 */
class DowVideoService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val prefs = Prefs(this@DowVideoService)
        private val handler = Handler(Looper.getMainLooper())
        private var player: MediaPlayer? = null
        private var prepared = false
        private var visible = false
        private var hasSurface = false
        private var attempts = 0
        private var position = 0
        private val retry = Runnable { start() }
        // Liberar el decodificador cuando el fondo no se ve, para no competir con otros reproductores.
        private val releaseIdle = Runnable { savePosition(); release() }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            prefs.sp.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onDestroy() {
            prefs.sp.unregisterOnSharedPreferenceChangeListener(this)
            handler.removeCallbacksAndMessages(null)
            release()
            super.onDestroy()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            hasSurface = true
            attempts = 0
            start()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            hasSurface = false
            handler.removeCallbacks(retry)
            savePosition()
            release()
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            if (v) {
                handler.removeCallbacks(releaseIdle)
                val p = player
                if (p == null) { attempts = 0; handler.removeCallbacks(retry); start() }
                else if (prepared) try { p.start() } catch (e: Exception) {}
            } else {
                try { if (prepared) player?.pause() } catch (e: Exception) {}
                handler.removeCallbacks(releaseIdle)
                handler.postDelayed(releaseIdle, 2000)
            }
        }

        override fun onSharedPreferenceChanged(sp: SharedPreferences?, key: String?) {
            if (key == "video_uri") handler.post {
                handler.removeCallbacks(retry)
                release(); position = 0; attempts = 0; start()
            }
        }

        private fun savePosition() {
            try { if (prepared) player?.let { position = it.currentPosition } } catch (e: Exception) {}
        }

        private fun start() {
            if (!hasSurface || player != null) return
            if (!visible && !isPreview && attempts == 0 && position > 0) return
            val uri = prefs.videoUri ?: return
            prepared = false
            try {
                val mp = MediaPlayer()
                player = mp
                mp.setDataSource(applicationContext, Uri.parse(uri))
                mp.setSurface(surfaceHolder.surface)
                mp.isLooping = true
                mp.setVolume(0f, 0f)
                mp.setOnPreparedListener {
                    prepared = true
                    attempts = 0
                    prefs.videoError = null
                    try { it.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING) } catch (e: Exception) {}
                    if (position > 0) try { it.seekTo(position) } catch (e: Exception) {}
                    if (visible || isPreview) it.start()
                }
                mp.setOnErrorListener { _, what, extra ->
                    handler.post { fail("código $what/$extra") }
                    true
                }
                mp.prepareAsync()
            } catch (e: SecurityException) {
                fail("sin permiso para leer el video")
            } catch (e: Exception) {
                fail(e.javaClass.simpleName)
            }
        }

        private fun fail(reason: String) {
            release()
            attempts++
            prefs.videoError = reason
            // Reintentos con espera creciente: suele ser un decodificador ocupado por otra vista previa.
            if (attempts <= 6) handler.postDelayed(retry, 700L * attempts)
        }

        private fun release() {
            player?.let { try { it.stop() } catch (e: Exception) {}; try { it.release() } catch (e: Exception) {} }
            player = null
            prepared = false
        }
    }
}
