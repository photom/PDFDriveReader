package com.hitsuji.pdfdrivereader.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests for the [PdfDocument] aggregate root.
 */
class PdfDocumentTest {

    /**
     * Verifies that [PdfDocument] instances are considered equal if they share the same id.
     */
    @Test
    fun `PdfDocument instances with identical IDs should be equal`() {
        val doc1 = PdfDocument(id = "uri1", fileName = "doc.pdf", totalPageCount = 10)
        val doc2 = PdfDocument(id = "uri1", fileName = "doc.pdf", totalPageCount = 20)
        val doc3 = PdfDocument(id = "uri2", fileName = "doc.pdf", totalPageCount = 10)

        assertEquals(doc1, doc2)
        assertNotEquals(doc1, doc3)
    }

    /**
     * Verifies that [PdfDocument] throws an exception when initialized with zero pages.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `initializing PdfDocument with zero pages should throw exception`() {
        PdfDocument(id = "uri1", fileName = "doc.pdf", totalPageCount = 0)
    }

    /**
     * Verifies that [PdfDocument] throws an exception when initialized with negative pages.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `initializing PdfDocument with negative pages should throw exception`() {
        PdfDocument(id = "uri1", fileName = "doc.pdf", totalPageCount = -1)
    }

    /**
     * Verifies that [PdfDocument] throws an exception when pageSizes does not match totalPageCount.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `initializing PdfDocument with mismatched pageSizes should throw exception`() {
        val sizes = listOf(PageDimension(100, 100))
        PdfDocument(id = "uri1", fileName = "doc.pdf", totalPageCount = 2, pageSizes = sizes)
    }

    /**
     * Verifies that [PdfDocument] initializes successfully when pageSizes matches totalPageCount.
     */
    @Test
    fun `initializing PdfDocument with correct pageSizes should succeed`() {
        val sizes = listOf(PageDimension(100, 100), PageDimension(200, 200))
        val doc = PdfDocument(id = "uri1", fileName = "doc.pdf", totalPageCount = 2, pageSizes = sizes)
        assertEquals(2, doc.pageSizes.size)
        assertEquals(100, doc.pageSizes[0].width)
    }
}
