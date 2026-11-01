package com.dowpaper.android.core

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * Copia la foto o el video elegidos al almacenamiento interno de la app.
 * Así el fondo no depende de permisos de otras apps (que pueden perderse al reiniciar).
 */
object MediaImport {
    private const val MAX_VIDEO_BYTES = 600L * 1024 * 1024

    private fun dir(ctx: Context) = File(ctx.filesDir, "media").apply { mkdirs() }

    fun isLocal(uri: String?): Boolean = uri != null && uri.startsWith("file:")

    private fun size(ctx: Context, uri: Uri): Long = try {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L
        } ?: -1L
    } catch (e: Exception) { -1L }

    /** Devuelve la Uri del archivo copiado, o null si no se pudo copiar. Bloqueante: usar en un hilo. */
    fun import(ctx: Context, uri: Uri, video: Boolean): Uri? {
        if (uri.scheme == "file") return uri
        if (video) {
            val s = size(ctx, uri)
            if (s > MAX_VIDEO_BYTES) return null
        }
        val prefix = if (video) "video_" else "photo_"
        val out = File(dir(ctx), prefix + System.currentTimeMillis() + if (video) ".mp4" else ".img")
        val tmp = File(out.path + ".part")
        return try {
            val input = ctx.contentResolver.openInputStream(uri) ?: return null
            input.use { i -> tmp.outputStream().use { o -> i.copyTo(o, 256 * 1024) } }
            if (tmp.length() == 0L || !tmp.renameTo(out)) { tmp.delete(); return null }
            dir(ctx).listFiles()?.forEach { f -> if (f.name.startsWith(prefix) && f != out) f.delete() }
            Uri.fromFile(out)
        } catch (e: Throwable) {
            tmp.delete(); out.delete(); null
        }
    }

    /** Migra fotos/videos guardados como enlaces externos (versiones anteriores) a copias internas. */
    fun migrate(ctx: Context, prefs: Prefs) {
        prefs.photoUri?.let { u -> if (!isLocal(u)) import(ctx, Uri.parse(u), false)?.let { prefs.photoUri = it.toString() } }
        prefs.videoUri?.let { u -> if (!isLocal(u)) import(ctx, Uri.parse(u), true)?.let { prefs.videoUri = it.toString() } }
    }
}
