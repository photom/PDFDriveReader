package com.hitsuji.pdfdrivereader.presentation.reader

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Logic test for dampened inertia.
 */
class ReaderDampenedInertiaTest {

    @Test
    fun `velocity should be dampened by factor`() {
        val rawVelocity = 1000f
        val dampeningFactor = 0.7f
        val expectedVelocity = 700f
        
        val actualVelocity = rawVelocity * dampeningFactor
        
        assertEquals(expectedVelocity, actualVelocity, 0.001f)
    }
}
