package com.hitsuji.pdfdrivereader.data.mapper

import com.hitsuji.pdfdrivereader.data.local.entity.DocumentMetadataEntity
import com.hitsuji.pdfdrivereader.domain.model.DocumentMetadata
import com.hitsuji.pdfdrivereader.domain.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the [DocumentMapper].
 */
class DocumentMapperTest {

    private val mapper = DocumentMapper()

    /**
     * Verifies that [DocumentMetadata] domain model is correctly mapped to [DocumentMetadataEntity].
     */
    @Test
    fun `toEntity should map all fields from domain to data entity`() {
        val domain = DocumentMetadata(
            id = "uri1",
            fileName = "test.pdf",
            locationPath = "/docs/",
            source = SourceType.LOCAL_STORAGE
        )
        
        val entity = mapper.toEntity(domain)
        
        assertEquals(domain.id, entity.fileUri)
        assertEquals(domain.fileName, entity.fileName)
        assertEquals(domain.locationPath, entity.locationPath)
        assertEquals(domain.source.name, entity.sourceType)
    }

    /**
     * Verifies that [DocumentMetadataEntity] is correctly mapped to [DocumentMetadata] domain model.
     */
    @Test
    fun `toDomain should map all fields from entity to domain model`() {
        val entity = DocumentMetadataEntity(
            fileUri = "uri1",
            fileName = "test.pdf",
            locationPath = "/docs/",
            sourceType = "LOCAL_STORAGE",
            lastModified = 123456789L
        )
        
        val domain = mapper.toDomain(entity)
        
        assertEquals(entity.fileUri, domain.id)
        assertEquals(entity.fileName, domain.fileName)
        assertEquals(entity.locationPath, domain.locationPath)
        assertEquals(entity.sourceType, domain.source.name)
    }
}
