package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Unit tests for the [SyncLocalLibraryUseCase].
 */
class SyncLocalLibraryUseCaseTest {

    private val repository: PdfRepository = mock()
    private val useCase = SyncLocalLibraryUseCase(repository)

    /**
     * Verifies that the use case correctly triggers the repository local sync operation.
     */
    @Test
    fun `invoke should delegate the local sync operation to the repository`() = runTest {
        useCase()
        verify(repository).syncLocal()
    }
}
