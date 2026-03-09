package com.hitsuji.pdfdrivereader.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the [ReadingSettings] value object.
 */
class ReadingSettingsTest {

    /**
     * Verifies that a new [ReadingSettings] instance is initialized with the correct default values.
     */
    @Test
    fun `ReadingSettings should be initialized with LTR, 1_0 zoom, and coverMode enabled by default`() {
        val settings = ReadingSettings()
        assertEquals(ReadingDirection.LTR, settings.direction)
        assertEquals(1.0f, settings.savedZoom)
        assertEquals(true, settings.isCoverModeEnabled)
    }

    /**
     * Verifies that two [ReadingSettings] instances with identical properties are considered equal.
     */
    @Test
    fun `two ReadingSettings instances with identical values should be equal`() {
        val s1 = ReadingSettings(ReadingDirection.RTL, 2.0f, false)
        val s2 = ReadingSettings(ReadingDirection.RTL, 2.0f, false)
        assertEquals(s1, s2)
    }
}
