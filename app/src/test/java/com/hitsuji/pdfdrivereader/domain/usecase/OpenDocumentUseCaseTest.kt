package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.model.PagePosition
import com.hitsuji.pdfdrivereader.domain.model.PdfDocument
import com.hitsuji.pdfdrivereader.domain.model.ReadingDirection
import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for the [OpenDocumentUseCase].
 */
class OpenDocumentUseCaseTest {

    private val repository: PdfRepository = mock()
    private val useCase = OpenDocumentUseCase(repository)

    /**
     * Verifies that the use case retrieves both the document metadata and its saved settings.
     */
    @Test
    fun `invoke should return document with restored settings when they exist`() = runTest {
        val uri = "uri1"
        val expectedDoc = PdfDocument(uri, 10)
        val savedPos = PagePosition(5, 1.5f)
        val savedDirection = ReadingDirection.RTL

        whenever(repository.getDocument(uri)) doReturn expectedDoc
        whenever(repository.getSavedPosition(uri)) doReturn savedPos
        whenever(repository.getSavedDirection(uri)) doReturn savedDirection

        val result = useCase(uri)

        assertEquals(expectedDoc, result.document)
        assertEquals(savedPos, result.position)
        assertEquals(savedDirection, result.direction)
    }

    /**
     * Verifies that the use case provides default settings when no saved state is found.
     */
    @Test
    fun `invoke should return document with default settings when no saved state exists`() = runTest {
        val uri = "uri1"
        val expectedDoc = PdfDocument(uri, 10)

        whenever(repository.getDocument(uri)) doReturn expectedDoc
        whenever(repository.getSavedPosition(uri)) doReturn null
        whenever(repository.getSavedDirection(uri)) doReturn null

        val result = useCase(uri)

        assertEquals(expectedDoc, result.document)
        assertEquals(PagePosition(0, 1.0f), result.position)
        assertEquals(ReadingDirection.LTR, result.direction)
    }
}
