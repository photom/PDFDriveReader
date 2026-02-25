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
}
