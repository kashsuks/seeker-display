package com.kashsuks.seekerdisplay.ui.nowplaying

import kotlin.math.pow

/** Gradient endpoints for the Now Playing background and the text colour that reads on them. */
data class BackgroundColors(val start: Int, val end: Int, val onBackground: Int)

object BackgroundColorsFactory {
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val MIN_CONTRAST = 4.5
    private const val MAX_DARKEN = 0.9f

    val Default = BackgroundColors(start = 0xFF2C2C2E.toInt(), end = 0xFF000000.toInt(), onBackground = WHITE)

    /**
     * Builds a moody gradient from the cover's swatches. Colours are pulled towards black so the
     * background stays easy on the eyes at night, and darkened further until white text on it
     * has at least [MIN_CONTRAST]:1 contrast. Bright covers therefore get a dimmer, but still tinted, background.
     */
    fun derive(dominant: Int?, vibrant: Int?, muted: Int?, darkMuted: Int?): BackgroundColors {
        val base = vibrant ?: dominant ?: muted ?: return Default
        val endBase = darkMuted ?: dominant ?: base

        var amount = 0.25f
        var start = mixWithBlack(base, amount)
        while (contrast(WHITE, start) < MIN_CONTRAST && amount < MAX_DARKEN) {
            amount += 0.05f
            start = mixWithBlack(base, amount)
        }
        // The far end of the gradient is always darker than the start.
        val end = mixWithBlack(endBase, maxOf(0.6f, amount + 0.25f))
        return BackgroundColors(start, end, WHITE)
    }

    fun luminance(color: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((color shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun mixWithBlack(color: Int, amount: Float): Int {
        val keep = 1f - amount
        val r = (((color shr 16) and 0xFF) * keep).toInt()
        val g = (((color shr 8) and 0xFF) * keep).toInt()
        val b = ((color and 0xFF) * keep).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
