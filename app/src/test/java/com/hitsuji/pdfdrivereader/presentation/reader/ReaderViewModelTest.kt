package com.hitsuji.pdfdrivereader.presentation.reader

import android.graphics.Bitmap
import com.hitsuji.pdfdrivereader.domain.usecase.GetPageImageUseCase
import com.hitsuji.pdfdrivereader.domain.usecase.OpenDocumentUseCase
import com.hitsuji.pdfdrivereader.domain.usecase.SaveReadingPositionUseCase
import com.hitsuji.pdfdrivereader.domain.usecase.SaveReadingDirectionUseCase
import com.hitsuji.pdfdrivereader.domain.usecase.OpenedDocument
import com.hitsuji.pdfdrivereader.domain.repository.AppConfigurationRepository
import com.hitsuji.pdfdrivereader.domain.model.PdfDocument
import com.hitsuji.pdfdrivereader.domain.model.PagePosition
import com.hitsuji.pdfdrivereader.domain.model.ReadingDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReaderViewModelTest {

    private val openDocumentUseCase: OpenDocumentUseCase = mock()
    private val saveReadingPositionUseCase: SaveReadingPositionUseCase = mock()
    private val saveReadingDirectionUseCase: SaveReadingDirectionUseCase = mock()
    private val getPageImageUseCase: GetPageImageUseCase = mock()
    private val appConfigRepository: AppConfigurationRepository = mock()
    private lateinit var viewModel: ReaderViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun `setup viewModel`() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun `tear down`() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleUI should flip visibility state`() = runTest {
        viewModel = ReaderViewModel(
            openDocumentUseCase, 
            saveReadingPositionUseCase, 
            saveReadingDirectionUseCase,
            getPageImageUseCase,
            appConfigRepository
        )
        
        assertFalse(viewModel.state.value.isUiVisible)
        
        viewModel.toggleUI()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isUiVisible)
        
        viewModel.toggleUI()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isUiVisible)
    }

    @Test
    fun `onPageChanged should update state and load cache`() = runTest {
        val mockBitmap: Bitmap = mock()
        val uri = "uri1"
        val mockDoc = PdfDocument(uri, 10)
        val openedDoc = OpenedDocument(mockDoc, PagePosition(0, 1.0f), ReadingDirection.LTR)
        
        whenever(openDocumentUseCase(any())) doReturn openedDoc
        whenever(getPageImageUseCase(any(), any(), any(), any())) doReturn mockBitmap
        whenever(appConfigRepository.saveLastUri(any())) doAnswer { }
        whenever(appConfigRepository.saveMode(any())) doAnswer { }
        
        viewModel = ReaderViewModel(
            openDocumentUseCase, 
            saveReadingPositionUseCase, 
            saveReadingDirectionUseCase,
            getPageImageUseCase,
            appConfigRepository
        )
        
        viewModel.loadDocument(uri)
        advanceUntilIdle()
        
        viewModel.onPageChanged(5)
        advanceUntilIdle()
        
        assertEquals(5, viewModel.state.value.currentPage)
        // Verify multiple pages around 5 were loaded (4, 5, 6)
        assertTrue(viewModel.state.value.pageCache.containsKey(4))
        assertTrue(viewModel.state.value.pageCache.containsKey(5))
        assertTrue(viewModel.state.value.pageCache.containsKey(6))
    }

    @Test
    fun `rapid onPageChanged should cancel previous caching jobs`() = runTest {
        val uri = "uri1"
        val mockDoc = PdfDocument(uri, 100)
        val openedDoc = OpenedDocument(mockDoc, PagePosition(0, 1.0f), ReadingDirection.LTR)
        
        whenever(openDocumentUseCase(any())) doReturn openedDoc
        whenever(getPageImageUseCase(any(), any(), any(), any())) doAnswer {
            // No need for thread sleep here, we just want to verify the latest window is present
            mock<Bitmap>()
        }
        whenever(appConfigRepository.saveLastUri(any())) doAnswer { }
        whenever(appConfigRepository.saveMode(any())) doAnswer { }
        
        viewModel = ReaderViewModel(
            openDocumentUseCase, 
            saveReadingPositionUseCase, 
            saveReadingDirectionUseCase,
            getPageImageUseCase,
            appConfigRepository
        )
        
        viewModel.loadDocument(uri)
        advanceUntilIdle()
        
        // Trigger multiple page changes rapidly
        viewModel.onPageChanged(10)
        viewModel.onPageChanged(20)
        viewModel.onPageChanged(30)
        
        advanceUntilIdle()
        
        // Final state should be page 30
        assertEquals(30, viewModel.state.value.currentPage)
        // Window should be 29, 30, 31
        assertTrue(viewModel.state.value.pageCache.containsKey(30))
        assertTrue(viewModel.state.value.pageCache.containsKey(29))
        assertTrue(viewModel.state.value.pageCache.containsKey(31))
        // Old pages (e.g., 10) should have been purged or never fully cached
        assertFalse(viewModel.state.value.pageCache.containsKey(10))
    }

    @Test
    fun `onDirectionChanged should update state and persist`() = runTest {
        val uri = "uri1"
        val mockDoc = PdfDocument(uri, 10)
        val openedDoc = OpenedDocument(mockDoc, PagePosition(0, 1.0f), ReadingDirection.LTR)
        
        whenever(openDocumentUseCase(any())) doReturn openedDoc
        whenever(appConfigRepository.saveLastUri(any())) doAnswer { }
        whenever(appConfigRepository.saveMode(any())) doAnswer { }
        
        viewModel = ReaderViewModel(
            openDocumentUseCase, 
            saveReadingPositionUseCase, 
            saveReadingDirectionUseCase,
            getPageImageUseCase,
            appConfigRepository
        )
        
        viewModel.loadDocument(uri)
        advanceUntilIdle()
        
        viewModel.onDirectionChanged(ReadingDirection.RTL)
        advanceUntilIdle()
        
        assertEquals(ReadingDirection.RTL, viewModel.state.value.direction)
        verify(saveReadingDirectionUseCase).invoke(eq(uri), eq(ReadingDirection.RTL))
    }

    @Test
    fun `loadDocument should set isLoading to false after successful load`() = runTest {
        val uri = "uri1"
        val mockDoc = PdfDocument(uri, 10)
        val openedDoc = OpenedDocument(mockDoc, PagePosition(0, 1.0f), ReadingDirection.LTR)
        val mockBitmap: Bitmap = mock()
        
        whenever(openDocumentUseCase(any())) doReturn openedDoc
        whenever(getPageImageUseCase(any(), any(), any(), any())) doReturn mockBitmap
        whenever(appConfigRepository.saveLastUri(any())) doAnswer { }
        whenever(appConfigRepository.saveMode(any())) doAnswer { }
        
        viewModel = ReaderViewModel(
            openDocumentUseCase, 
            saveReadingPositionUseCase, 
            saveReadingDirectionUseCase,
            getPageImageUseCase,
            appConfigRepository
        )
        
        viewModel.loadDocument(uri)
        advanceUntilIdle()
        
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `loadDocument should set isLoading to false and set error message on failure`() = runTest {
        val uri = "uri1"
        whenever(openDocumentUseCase(any())) doAnswer { throw RuntimeException("IO Error") }
        
        viewModel = ReaderViewModel(
            openDocumentUseCase, 
            saveReadingPositionUseCase, 
            saveReadingDirectionUseCase,
            getPageImageUseCase,
            appConfigRepository
        )
        
        viewModel.loadDocument(uri)
        advanceUntilIdle()
        
        assertFalse(viewModel.state.value.isLoading)
        assertEquals("IO Error", viewModel.state.value.errorMessage)
    }
}
