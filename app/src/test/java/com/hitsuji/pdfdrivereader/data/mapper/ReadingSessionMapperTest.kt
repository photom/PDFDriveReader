package com.hitsuji.pdfdrivereader.data.mapper

import com.hitsuji.pdfdrivereader.data.local.entity.ReadingSessionEntity
import com.hitsuji.pdfdrivereader.domain.model.PagePosition
import com.hitsuji.pdfdrivereader.domain.model.ReadingDirection
import com.hitsuji.pdfdrivereader.domain.model.ReadingSettings
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the [ReadingSessionMapper].
 */
class ReadingSessionMapperTest {

    private val mapper = ReadingSessionMapper()

    /**
     * Verifies that reading settings are correctly mapped to [ReadingSessionEntity].
     */
    @Test
    fun `toEntity should map settings and position to data entity`() {
        val fileUri = "uri1"
        val position = PagePosition(10, 2.0f)
        val settings = ReadingSettings(ReadingDirection.TTB, 2.0f)
        
        val entity = mapper.toEntity(fileUri, position, settings)
        
        assertEquals(fileUri, entity.fileUri)
        assertEquals(10, entity.currentPage)
        assertEquals("TTB", entity.readingDirection)
        assertEquals(2.0f, entity.zoomLevel)
    }

    /**
     * Verifies that [ReadingSessionEntity] is correctly mapped back to domain objects.
     */
    @Test
    fun `toDomain should extract position and settings from entity`() {
        val entity = ReadingSessionEntity(
            fileUri = "uri1",
            currentPage = 5,
            readingDirection = "RTL",
            zoomLevel = 1.5f
        )
        
        val (pos, settings) = mapper.toDomain(entity)
        
        assertEquals(5, pos.pageIndex)
        assertEquals(1.5f, pos.zoomLevel)
        assertEquals(ReadingDirection.RTL, settings.direction)
        assertEquals(1.5f, settings.savedZoom)
    }
}
