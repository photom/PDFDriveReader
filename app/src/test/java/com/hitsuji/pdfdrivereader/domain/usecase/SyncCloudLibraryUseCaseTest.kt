package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Unit tests for the [SyncCloudLibraryUseCase].
 */
class SyncCloudLibraryUseCaseTest {

    private val repository: PdfRepository = mock()
    private val useCase = SyncCloudLibraryUseCase(repository)

    /**
     * Verifies that the use case correctly triggers the repository sync operation.
     */
    @Test
    fun `invoke should delegate the cloud sync operation to the repository`() = runTest {
        useCase()
        verify(repository).syncCloud()
    }
}
