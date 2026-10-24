package com.kashsuks.seekerdisplay.ui.nowplaying

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundColorsTest {
    private val white = 0xFFFFFFFF.toInt()

    @Test
    fun noSwatchesFallsBackToDefault() {
        assertEquals(BackgroundColorsFactory.Default, BackgroundColorsFactory.derive(null, null, null, null))
    }

    @Test
    fun darkCoverGetsWhiteText() {
        val colors = BackgroundColorsFactory.derive(0xFF203060.toInt(), 0xFF3050A0.toInt(), null, 0xFF101830.toInt())
        assertEquals(white, colors.onBackground)
    }

    @Test
    fun brightCoverIsDimmedUntilWhiteTextIsReadable() {
        val colors = BackgroundColorsFactory.derive(0xFFFFE600.toInt(), 0xFFFFE600.toInt(), null, 0xFFB0A000.toInt())
        assertEquals(white, colors.onBackground)
        assertTrue(BackgroundColorsFactory.contrast(white, colors.start) >= 4.5)
        assertTrue(BackgroundColorsFactory.contrast(white, colors.end) >= 4.5)
    }

    @Test
    fun gradientIsDarkerThanTheSourceColour() {
        val source = 0xFFE04040.toInt()
        val colors = BackgroundColorsFactory.derive(source, source, null, source)
        assertTrue(BackgroundColorsFactory.luminance(colors.start) < BackgroundColorsFactory.luminance(source))
        assertTrue(BackgroundColorsFactory.luminance(colors.end) < BackgroundColorsFactory.luminance(colors.start))
    }

    @Test
    fun luminanceOfBlackAndWhite() {
        assertEquals(0.0, BackgroundColorsFactory.luminance(0xFF000000.toInt()), 1e-9)
        assertEquals(1.0, BackgroundColorsFactory.luminance(white), 1e-6)
    }
}
