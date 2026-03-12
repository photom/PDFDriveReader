package com.hitsuji.pdfdrivereader.presentation.reader

import androidx.compose.ui.unit.Velocity
import org.junit.Assert.assertEquals
import org.junit.Test

class InertiaScalingTest {

    @Test
    fun `velocity is NOT scaled by zoom level to maintain 1 to 1 screen panning inertia`() {
        val baseVelocity = Velocity(1000f, -500f)
        
        // At 1x zoom, the velocity should remain unchanged
        val scale1x = 1.0f
        val velocity1x = calculateTargetVelocity(baseVelocity, scale1x)
        assertEquals(1000f, velocity1x.x, 0.01f)
        assertEquals(-500f, velocity1x.y, 0.01f)
        
        // At 3x zoom, the velocity should also remain unchanged (1:1 panning)
        val scale3x = 3.0f
        val velocity3x = calculateTargetVelocity(baseVelocity, scale3x)
        assertEquals(1000f, velocity3x.x, 0.01f)
        assertEquals(-500f, velocity3x.y, 0.01f)
        
        // At 5x zoom, the velocity should also remain unchanged
        val scale5x = 5.0f
        val velocity5x = calculateTargetVelocity(baseVelocity, scale5x)
        assertEquals(1000f, velocity5x.x, 0.01f)
        assertEquals(-500f, velocity5x.y, 0.01f)
    }
}
