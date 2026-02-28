package com.hitsuji.pdfdrivereader.presentation.reader

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.generateDecayAnimationSpec
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Logic-only test for fling/momentum calculations.
 */
class ReaderFlingTest {

    @Test
    fun `decay animation should produce decreasing values over time`() {
        val decay = exponentialDecay<Float>()
        val initialVelocity = 1000f
        val initialValue = 0f
        
        // At t=0, value is initialValue
        // At t > 0, value should be moving in the direction of velocity
        
        val spec = decay.generateDecayAnimationSpec<Float>()
        
        val valueAt100ms = spec.getValueFromNanos(100_000_000L, initialValue, initialVelocity)
        val valueAt200ms = spec.getValueFromNanos(200_000_000L, initialValue, initialVelocity)
        
        assertTrue("Value at 200ms should be further than at 100ms", valueAt200ms > valueAt100ms)
        
        val velAt100ms = spec.getVelocityFromNanos(100_000_000L, initialValue, initialVelocity)
        val velAt200ms = spec.getVelocityFromNanos(200_000_000L, initialValue, initialVelocity)
        
        assertTrue("Velocity must decrease over time (friction)", velAt200ms < velAt100ms)
    }
}
