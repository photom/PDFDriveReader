package com.hitsuji.pdfdrivereader.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Unit tests for the [DocumentMetadata] entity.
 */
class DocumentMetadataTest {

    /**
     * Verifies that [DocumentMetadata] instances are considered equal if they share the same id,
     * regardless of other property values.
     */
    @Test
    fun `DocumentMetadata instances with identical IDs should be equal`() {
        val doc1 = DocumentMetadata(
            id = "uri1",
            fileName = "file1.pdf",
            locationPath = "/local/",
            source = SourceType.LOCAL_STORAGE
        )
        val doc2 = DocumentMetadata(
            id = "uri1",
            fileName = "different_name.pdf",
            locationPath = "/other/",
            source = SourceType.GOOGLE_DRIVE
        )
        val doc3 = DocumentMetadata(
            id = "uri2",
            fileName = "file1.pdf",
            locationPath = "/local/",
            source = SourceType.LOCAL_STORAGE
        )

        assertEquals(doc1, doc2)
        assertNotEquals(doc1, doc3)
    }

    /**
     * Verifies that the isCached property defaults to false and can be set correctly.
     */
    @Test
    fun `isCached property should reflect the provided value`() {
        val doc = DocumentMetadata("id", "name", "path", SourceType.GOOGLE_DRIVE, isCached = true)
        assertTrue(doc.isCached)
        
        val docDefault = DocumentMetadata("id", "name", "path", SourceType.GOOGLE_DRIVE)
        assertFalse(docDefault.isCached)
    }
}
