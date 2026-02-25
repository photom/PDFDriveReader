package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.model.PagePosition
import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Unit tests for the [SaveReadingPositionUseCase].
 */
class SaveReadingPositionUseCaseTest {

    private val repository: PdfRepository = mock()
    private val useCase = SaveReadingPositionUseCase(repository)

    /**
     * Verifies that the use case correctly delegates the save operation to the repository.
     */
    @Test
    fun `invoke should delegate the save position operation to the repository`() = runTest {
        val uri = "uri1"
        val position = PagePosition(10, 2.0f)
        
        useCase(uri, position)
        
        verify(repository).savePosition(uri, position)
    }
}
