package com.jellymusic.player.ui.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color

/**
 * Extracts a soft 3-color palette from an album artwork bitmap,
 * used for Apple-style ambient backdrop diffusion and player mood lighting.
 */
fun extractPaletteFromBitmap(bitmap: Bitmap): Triple<Color, Color, Color> {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val hsv = FloatArray(3)
    data class HsvColor(val argb: Int, val h: Float, val s: Float, val v: Float)

    val validColors = ArrayList<HsvColor>(pixels.size)
    for (pixel in pixels) {
        val alpha = (pixel ushr 24) and 0xFF
        if (alpha < 128) continue
        android.graphics.Color.colorToHSV(pixel, hsv)
        val s = hsv[1]
        val v = hsv[2]
        if (v > 0.12f && (s > 0.15f || (v > 0.25f && v < 0.90f))) {
            validColors.add(HsvColor(pixel, hsv[0], s, v))
        }
    }

    if (validColors.isEmpty()) {
        return Triple(AmbientAmber, AmbientIndigo, AmbientDeepBlue)
    }

    val c1 = validColors.maxByOrNull { it.s * 1.6f + it.v } ?: validColors.first()
    val c2 = validColors
        .filter {
            val diff = Math.abs(it.h - c1.h)
            val hueDiff = if (diff > 180f) 360f - diff else diff
            hueDiff >= 30f && it.s > 0.18f
        }
        .maxByOrNull { it.s * 1.3f + it.v }
        ?: validColors.maxByOrNull {
            val diff = Math.abs(it.h - c1.h)
            if (diff > 180f) 360f - diff else diff
        }
        ?: c1

    val c3 = validColors
        .filter { it.v in 0.15f..0.50f }
        .maxByOrNull { it.s }
        ?: validColors.minByOrNull { it.v }
        ?: c1

    return Triple(
        Color(c1.argb),
        Color(c2.argb),
        Color(c3.argb)
    )
}
