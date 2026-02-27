package com.hitsuji.pdfdrivereader.presentation.reader

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Logic-only test for the zoom focal point calculation.
 * Verifies the math required to keep a point stable during scaling.
 */
class ReaderFocalPointZoomTest {

    @Test
    fun `calculateNewOffset should keep focal point stationary`() {
        val oldScale = 1.0f
        val newScale = 2.0f
        val focalPoint = Offset(100f, 100f) // User's fingers midpoint in viewport
        val oldOffset = Offset.Zero // Initial translation

        // Math formula for focal-point zoom:
        // newOffset = focalPoint - (focalPoint - oldOffset) * (newScale / oldScale)
        
        val newOffset = focalPoint - (focalPoint - oldOffset) * (newScale / oldScale)

        // Verification: If we apply (point - newOffset) / newScale, it should map back to the same
        // logical coordinate as (point - oldOffset) / oldScale
        val logicalCoordBefore = (focalPoint - oldOffset) / oldScale
        val logicalCoordAfter = (focalPoint - newOffset) / newScale

        assertEquals("Logical coordinate under focal point must be identical", 
            logicalCoordBefore.x, logicalCoordAfter.x, 0.001f)
        assertEquals("Logical coordinate under focal point must be identical", 
            logicalCoordBefore.y, logicalCoordAfter.y, 0.001f)
    }
}
