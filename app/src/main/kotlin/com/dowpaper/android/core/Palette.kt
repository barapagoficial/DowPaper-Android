package com.dowpaper.android.core

class Palette(val name: String, val bgTop: Int, val bgBottom: Int, val colors: IntArray)

object Palettes {
    private fun c(v: Long): Int = v.toInt()

    val all: List<Palette> = listOf(
        Palette("Boreal", c(0xFF040A18), c(0xFF0B1F35),
            intArrayOf(c(0xFF3DFFB0), c(0xFF29D3FF), c(0xFF8A5CFF), c(0xFFC56CFF), c(0xFFE0FFF4))),
        Palette("Atardecer", c(0xFF1A0B2E), c(0xFF46143F),
            intArrayOf(c(0xFFFF6B6B), c(0xFFFFA94D), c(0xFFFFD43B), c(0xFFF06595), c(0xFFFFE8CC))),
        Palette("Océano", c(0xFF021526), c(0xFF043A6E),
            intArrayOf(c(0xFF00B4D8), c(0xFF48CAE4), c(0xFF90E0EF), c(0xFF4361EE), c(0xFFCAF0F8))),
        Palette("Neón", c(0xFF0A0014), c(0xFF17002E),
            intArrayOf(c(0xFFFF2CDF), c(0xFF00F0FF), c(0xFF7B2CFF), c(0xFFFFE600), c(0xFFFFFFFF))),
        Palette("Lava", c(0xFF120404), c(0xFF2E0A05),
            intArrayOf(c(0xFFFF3D00), c(0xFFFF9100), c(0xFFFFC400), c(0xFFFF1744), c(0xFFFFF3E0))),
        Palette("Bosque", c(0xFF031A12), c(0xFF0A2E1F),
            intArrayOf(c(0xFF9BE564), c(0xFFD7F75B), c(0xFF43AA8B), c(0xFFF9F871), c(0xFFE8FFD8))),
        Palette("Lavanda", c(0xFF140F2E), c(0xFF2C2052),
            intArrayOf(c(0xFFC3AED6), c(0xFFF8B6D2), c(0xFFA0C4FF), c(0xFFFFD6FF), c(0xFFFFFFFF))),
        Palette("Menta", c(0xFF03141A), c(0xFF0B2C30),
            intArrayOf(c(0xFF64FFDA), c(0xFF1DE9B6), c(0xFFB9F6CA), c(0xFF18FFFF), c(0xFFE0F7FA))),
        Palette("Oro", c(0xFF0D0A05), c(0xFF241A0A),
            intArrayOf(c(0xFFFFD166), c(0xFFF4A261), c(0xFFFFE8A3), c(0xFFE76F51), c(0xFFFFF8E7))),
        Palette("Mono", c(0xFF050505), c(0xFF161616),
            intArrayOf(c(0xFFFFFFFF), c(0xFFBDBDBD), c(0xFF8A8A8A), c(0xFFE0E0E0), c(0xFFFFFFFF)))
    )

    fun get(i: Int): Palette = all[((i % all.size) + all.size) % all.size]
}
