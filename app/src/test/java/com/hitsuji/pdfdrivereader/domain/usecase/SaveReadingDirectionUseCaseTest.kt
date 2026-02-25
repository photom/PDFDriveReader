package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.model.ReadingDirection
import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Unit tests for the [SaveReadingDirectionUseCase].
 */
class SaveReadingDirectionUseCaseTest {

    private val repository: PdfRepository = mock()
    private val useCase = SaveReadingDirectionUseCase(repository)

    /**
     * Verifies that the use case correctly delegates the save operation to the repository.
     */
    @Test
    fun `invoke should delegate the save direction operation to the repository`() = runTest {
        val uri = "uri1"
        val direction = ReadingDirection.TTB
        
        useCase(uri, direction)
        
        verify(repository).saveDirection(uri, direction)
    }
}
