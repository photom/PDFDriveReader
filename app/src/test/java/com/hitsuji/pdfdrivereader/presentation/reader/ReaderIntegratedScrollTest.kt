package com.hitsuji.pdfdrivereader.presentation.reader

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Logic test for integrated scrolling (spill-over).
 */
class ReaderIntegratedScrollTest {

    @Test
    fun `pan delta should spill over to list scroll when at edge`() {
        val currentOffset = 500f
        val maxX = 500f
        val panDelta = 100f
        
        // We want to pan 100 units.
        // Offset is already at maxX.
        // So offset stays at 500, and 100 units spill over to list.
        
        val newOffset = (currentOffset + panDelta).coerceIn(-maxX, maxX)
        val spillOver = (currentOffset + panDelta) - newOffset
        
        assertEquals(500f, newOffset)
        assertEquals(100f, spillOver)
    }
}
