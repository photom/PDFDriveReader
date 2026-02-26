package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.model.DomainResult
import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
    fun `invoke should delegate the local sync operation to the repository and return Success`() = runTest {
        val result = useCase()
        verify(repository).syncLocal()
        assertTrue(result is DomainResult.Success)
    }

    /**
     * Verifies that the use case handles exceptions gracefully.
     */
    @Test
    fun `invoke should return Error when repository throws exception`() = runTest {
        whenever(repository.syncLocal()) doAnswer { throw RuntimeException("IO Error") }
        
        val result = useCase()
        
        assertTrue(result is DomainResult.Error)
        assertEquals("Local scan failed: IO Error", (result as DomainResult.Error).message)
    }
}
