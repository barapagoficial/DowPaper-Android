package com.dowpaper.android.core

import android.content.Context
import android.content.SharedPreferences

class Prefs(ctx: Context) {
    val sp: SharedPreferences = ctx.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var active: String
        get() = sp.getString(K_ACTIVE, "aurora") ?: "aurora"
        set(v) = sp.edit().putString(K_ACTIVE, v).apply()

    fun palette(id: String, def: Int): Int = sp.getInt("pal_$id", def)
    fun setPalette(id: String, v: Int) = sp.edit().putInt("pal_$id", v).apply()
    fun speed(id: String): Float = sp.getFloat("spd_$id", 1f)
    fun setSpeed(id: String, v: Float) = sp.edit().putFloat("spd_$id", v).apply()
    fun density(id: String): Float = sp.getFloat("den_$id", 1f)
    fun setDensity(id: String, v: Float) = sp.edit().putFloat("den_$id", v).apply()

    var touch: Boolean
        get() = sp.getBoolean("touch", true)
        set(v) = sp.edit().putBoolean("touch", v).apply()
    var parallax: Boolean
        get() = sp.getBoolean("parallax", true)
        set(v) = sp.edit().putBoolean("parallax", v).apply()
    var doubleTap: Boolean
        get() = sp.getBoolean("dtap", true)
        set(v) = sp.edit().putBoolean("dtap", v).apply()
    var fps: Int
        get() = sp.getInt("fps", 60)
        set(v) = sp.edit().putInt("fps", v).apply()
    var saver: Boolean
        get() = sp.getBoolean("saver", true)
        set(v) = sp.edit().putBoolean("saver", v).apply()
    var rotateMinutes: Int
        get() = sp.getInt("rotate", 0)
        set(v) = sp.edit().putInt("rotate", v).apply()
    var rotateFavs: Boolean
        get() = sp.getBoolean("rotate_favs", false)
        set(v) = sp.edit().putBoolean("rotate_favs", v).apply()
    var lastRotate: Long
        get() = sp.getLong("last_rotate", 0L)
        set(v) = sp.edit().putLong("last_rotate", v).apply()

    var photoUri: String?
        get() = sp.getString("photo_uri", null)
        set(v) = sp.edit().putString("photo_uri", v).apply()
    var photoEffect: String
        get() = sp.getString("photo_fx", "bokeh") ?: "bokeh"
        set(v) = sp.edit().putString("photo_fx", v).apply()
    var photoDim: Float
        get() = sp.getFloat("photo_dim", 0.2f)
        set(v) = sp.edit().putFloat("photo_dim", v).apply()
    var videoUri: String?
        get() = sp.getString("video_uri", null)
        set(v) = sp.edit().putString("video_uri", v).apply()

    val favorites: Set<String>
        get() = HashSet(sp.getStringSet("favs", emptySet()) ?: emptySet())

    fun isFav(id: String) = favorites.contains(id)

    fun toggleFav(id: String): Boolean {
        val s = favorites.toMutableSet()
        val now = if (s.contains(id)) { s.remove(id); false } else { s.add(id); true }
        sp.edit().putStringSet("favs", s).apply()
        return now
    }

    companion object {
        const val NAME = "dowpaper"
        const val K_ACTIVE = "active"
    }
}
