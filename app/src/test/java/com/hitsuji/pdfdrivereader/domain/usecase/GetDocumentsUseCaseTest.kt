package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.model.DocumentMetadata
import com.hitsuji.pdfdrivereader.domain.model.SourceType
import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for the [GetDocumentsUseCase].
 */
class GetDocumentsUseCaseTest {

    private val repository: PdfRepository = mock()
    private val useCase = GetDocumentsUseCase(repository)

    /**
     * Verifies that the use case returns the document list provided by the repository.
     */
    @Test
    fun `invoke should return the reactive document stream from the repository`() = runTest {
        val expectedDocs = listOf(
            DocumentMetadata("1", "a.pdf", "/", SourceType.LOCAL_STORAGE)
        )
        whenever(repository.getDocuments()) doReturn flowOf(expectedDocs)

        val result = useCase().first()

        assertEquals(expectedDocs, result)
    }

    /**
     * Verifies that the use case sorts the document list alphabetically by locationPath and then by fileName.
     */
    @Test
    fun `invoke should return documents sorted by path then by name`() = runTest {
        val doc1 = DocumentMetadata("1", "z.pdf", "/b/", SourceType.LOCAL_STORAGE)
        val doc2 = DocumentMetadata("2", "a.pdf", "/b/", SourceType.LOCAL_STORAGE)
        val doc3 = DocumentMetadata("3", "m.pdf", "/a/", SourceType.LOCAL_STORAGE)
        
        val unsortedDocs = listOf(doc1, doc2, doc3)
        whenever(repository.getDocuments()) doReturn flowOf(unsortedDocs)

        val result = useCase().first()

        // Expected Order:
        // 1. /a/ - m.pdf (doc3)
        // 2. /b/ - a.pdf (doc2)
        // 3. /b/ - z.pdf (doc1)
        assertEquals(doc3, result[0])
        assertEquals(doc2, result[1])
        assertEquals(doc1, result[2])
    }
}
