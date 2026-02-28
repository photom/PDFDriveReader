package com.hitsuji.pdfdrivereader.presentation.reader

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Logic-only test for the edge clamping math.
 * Verifies that the viewing area stays within the page content when zoomed.
 */
class ReaderEdgeClampingTest {

    @Test
    fun `offset should be strictly clamped to prevent showing background space`() {
        val scale = 2.0f
        val viewportSize = Size(1000f, 2000f)
        
        // At scale 2.0, the logical content (1000x2000) becomes 2000x4000.
        // The center is at 500, 1000.
        // The extra width is (2.0 - 1) * 1000 = 1000.
        // The maxX translation from center is 1000 / 2 = 500.
        
        val extraWidth = (scale - 1) * viewportSize.width
        val extraHeight = (scale - 1) * viewportSize.height
        val maxX = extraWidth / 2f
        val maxY = extraHeight / 2f
        
        // Case 1: Try to pan too far left
        val requestedOffsetLeft = Offset(-600f, 0f)
        val clampedOffsetLeft = Offset(
            requestedOffsetLeft.x.coerceIn(-maxX, maxX),
            requestedOffsetLeft.y.coerceIn(-maxY, maxY)
        )
        assertEquals("Offset X must be clamped to -maxX", -500f, clampedOffsetLeft.x)

        // Case 2: Try to pan too far right
        val requestedOffsetRight = Offset(600f, 0f)
        val clampedOffsetRight = Offset(
            requestedOffsetRight.x.coerceIn(-maxX, maxX),
            requestedOffsetRight.y.coerceIn(-maxY, maxY)
        )
        assertEquals("Offset X must be clamped to maxX", 500f, clampedOffsetRight.x)
    }
}
