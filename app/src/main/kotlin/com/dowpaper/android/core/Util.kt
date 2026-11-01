package com.dowpaper.android.core

import android.content.Context
import kotlin.math.floor

fun Context.dp(v: Float): Float = v * resources.displayMetrics.density
fun Context.dpi(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
fun Context.sp(v: Float): Float = v * resources.displayMetrics.scaledDensity

object Col {
    fun alpha(c: Int, a: Float): Int {
        val aa = (a.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        return (c and 0x00FFFFFF) or (aa shl 24)
    }

    fun lerp(a: Int, b: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        val aa = (a ushr 24) and 0xFF; val ar = (a shr 16) and 0xFF; val ag = (a shr 8) and 0xFF; val ab = a and 0xFF
        val ba = (b ushr 24) and 0xFF; val br = (b shr 16) and 0xFF; val bg = (b shr 8) and 0xFF; val bb = b and 0xFF
        return ((aa + (ba - aa) * k).toInt() shl 24) or ((ar + (br - ar) * k).toInt() shl 16) or
            ((ag + (bg - ag) * k).toInt() shl 8) or (ab + (bb - ab) * k).toInt()
    }

    fun darken(c: Int, t: Float): Int = lerp(c, 0xFF000000.toInt(), t)
    fun lighten(c: Int, t: Float): Int = lerp(c, 0xFFFFFFFF.toInt(), t)
}

fun lerpF(a: Float, b: Float, t: Float): Float = a + (b - a) * t

fun smooth(e0: Float, e1: Float, x: Float): Float {
    val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** Ruido de valor suave, determinista y sin asignaciones. Rango aproximado -1..1 */
object Noise {
    private val perm = IntArray(512)

    init {
        val p = IntArray(256) { it }
        val r = java.util.Random(1337)
        for (i in 255 downTo 1) {
            val j = r.nextInt(i + 1)
            val t = p[i]; p[i] = p[j]; p[j] = t
        }
        for (i in 0 until 512) perm[i] = p[i and 255]
    }

    private fun g(h: Int): Float = perm[h and 511] / 127.5f - 1f

    fun n1(x: Float): Float {
        val fi = floor(x)
        val i = fi.toInt()
        val f = x - fi
        val u = f * f * (3f - 2f * f)
        val a = g(i and 255)
        val b = g((i + 1) and 255)
        return a + (b - a) * u
    }

    fun n2(x: Float, y: Float): Float {
        val fx = floor(x); val fy = floor(y)
        val xi = fx.toInt() and 255; val yi = fy.toInt() and 255
        val xf = x - fx; val yf = y - fy
        val u = xf * xf * (3f - 2f * xf)
        val v = yf * yf * (3f - 2f * yf)
        val aa = g(perm[xi] + yi)
        val ba = g(perm[xi + 1] + yi)
        val ab = g(perm[xi] + yi + 1)
        val bb = g(perm[xi + 1] + yi + 1)
        val x1 = aa + (ba - aa) * u
        val x2 = ab + (bb - ab) * u
        return x1 + (x2 - x1) * v
    }
}
