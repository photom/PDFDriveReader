package com.hitsuji.pdfdrivereader.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests for the [PagePosition] value object.
 */
class PagePositionTest {

    /**
     * Verifies that two [PagePosition] instances with identical values are considered equal.
     */
    @Test
    fun `two PagePosition instances with identical values should be equal`() {
        val pos1 = PagePosition(pageIndex = 5, zoomLevel = 1.5f)
        val pos2 = PagePosition(pageIndex = 5, zoomLevel = 1.5f)
        val pos3 = PagePosition(pageIndex = 6, zoomLevel = 1.5f)

        assertEquals(pos1, pos2)
        assertNotEquals(pos1, pos3)
    }

    /**
     * Verifies that [PagePosition] throws an exception when initialized with a negative page index.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `initializing PagePosition with negative pageIndex should throw exception`() {
        PagePosition(pageIndex = -1, zoomLevel = 1.0f)
    }

    /**
     * Verifies that [PagePosition] throws an exception when initialized with a zoom level below 1.0.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `initializing PagePosition with zoomLevel below 1_0 should throw exception`() {
        PagePosition(pageIndex = 0, zoomLevel = 0.9f)
    }

    /**
     * Verifies that [PagePosition] throws an exception when initialized with a zoom level above 5.0.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `initializing PagePosition with zoomLevel above 5_0 should throw exception`() {
        PagePosition(pageIndex = 0, zoomLevel = 5.1f)
    }
}
