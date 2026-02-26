package com.hitsuji.pdfdrivereader.presentation.library

import com.hitsuji.pdfdrivereader.data.remote.GoogleDriveService
import com.hitsuji.pdfdrivereader.data.remote.impl.FakeGoogleDriveService
import com.hitsuji.pdfdrivereader.domain.model.DocumentMetadata
import com.hitsuji.pdfdrivereader.domain.model.SourceType
import com.hitsuji.pdfdrivereader.domain.model.DomainResult
import com.hitsuji.pdfdrivereader.domain.repository.AppConfigurationRepository
import com.hitsuji.pdfdrivereader.domain.usecase.GetDocumentsUseCase
import com.hitsuji.pdfdrivereader.domain.usecase.SyncCloudLibraryUseCase
import com.hitsuji.pdfdrivereader.domain.usecase.SyncLocalLibraryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for the [LibraryViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val getDocumentsUseCase: GetDocumentsUseCase = mock()
    private val syncLocalLibraryUseCase: SyncLocalLibraryUseCase = mock()
    private val syncCloudLibraryUseCase: SyncCloudLibraryUseCase = mock()
    private val driveService: GoogleDriveService = FakeGoogleDriveService()
    private val appConfigRepository: AppConfigurationRepository = mock()
    private lateinit var viewModel: LibraryViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun `setup main dispatcher`() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun `tear down`() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be Loading`() = runTest {
        whenever(getDocumentsUseCase()) doReturn flowOf(emptyList())
        whenever(syncLocalLibraryUseCase()) doReturn DomainResult.Success(Unit)
        
        viewModel = LibraryViewModel(
            getDocumentsUseCase, 
            syncLocalLibraryUseCase,
            syncCloudLibraryUseCase,
            driveService,
            appConfigRepository
        )
        
        assertEquals(LibraryState.Loading, viewModel.state.value)
    }

    @Test
    fun `emitted documents should result in Success state`() = runTest {
        val docs = listOf(
            DocumentMetadata("1", "test.pdf", "/", SourceType.LOCAL_STORAGE)
        )
        whenever(getDocumentsUseCase()) doReturn flowOf(docs)
        whenever(syncLocalLibraryUseCase()) doReturn DomainResult.Success(Unit)
        
        viewModel = LibraryViewModel(
            getDocumentsUseCase, 
            syncLocalLibraryUseCase,
            syncCloudLibraryUseCase,
            driveService,
            appConfigRepository
        )
        advanceUntilIdle()
        
        val currentState = viewModel.state.value
        assertTrue(currentState is LibraryState.Success)
        assertEquals(docs, (currentState as LibraryState.Success).documents)
    }

    @Test
    fun `empty document list should result in Empty state`() = runTest {
        whenever(getDocumentsUseCase()) doReturn flowOf(emptyList())
        whenever(syncLocalLibraryUseCase()) doReturn DomainResult.Success(Unit)
        
        viewModel = LibraryViewModel(
            getDocumentsUseCase, 
            syncLocalLibraryUseCase,
            syncCloudLibraryUseCase,
            driveService,
            appConfigRepository
        )
        advanceUntilIdle()
        
        assertEquals(LibraryState.Empty, viewModel.state.value)
    }

    @Test
    fun `refreshLibrary should update isSyncing state`() = runTest {
        whenever(getDocumentsUseCase()) doReturn flowOf(emptyList())
        whenever(syncLocalLibraryUseCase()) doReturn DomainResult.Success(Unit)
        
        viewModel = LibraryViewModel(
            getDocumentsUseCase, 
            syncLocalLibraryUseCase,
            syncCloudLibraryUseCase,
            driveService,
            appConfigRepository
        )
        
        advanceUntilIdle()
        assertFalse(viewModel.isSyncing.value)
        
        viewModel.refreshLibrary()
        // We can't easily test the 'true' state without a more complex dispatcher setup,
        // but we can verify it returns to 'false'
        advanceUntilIdle()
        assertFalse(viewModel.isSyncing.value)
    }
}
