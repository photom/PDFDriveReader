package com.hitsuji.pdfdrivereader.presentation.reader

import android.graphics.Bitmap
import com.hitsuji.pdfdrivereader.domain.usecase.GetPageImageUseCase
import com.hitsuji.pdfdrivereader.domain.usecase.GetPageSizeUseCase
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
    private val getPageSizeUseCase: GetPageSizeUseCase = mock()
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
            getPageSizeUseCase,
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
        whenever(getPageSizeUseCase(any(), any())) doReturn (100 to 200)
        whenever(getPageImageUseCase(any(), any(), any(), any())) doReturn mockBitmap
        whenever(appConfigRepository.saveLastUri(any())) doAnswer { }
        whenever(appConfigRepository.saveMode(any())) doAnswer { }
        
        viewModel = ReaderViewModel(
            openDocumentUseCase, 
            saveReadingPositionUseCase, 
            saveReadingDirectionUseCase,
            getPageImageUseCase,
            getPageSizeUseCase,
            appConfigRepository
        )
        
        viewModel.loadDocument(uri)
        advanceUntilIdle()
        
        viewModel.onPageChanged(5)
        advanceUntilIdle()
        
        assertEquals(5, viewModel.state.value.currentPage)
        assertTrue(viewModel.state.value.pageCache.containsKey(4))
        assertTrue(viewModel.state.value.pageCache.containsKey(5))
        assertTrue(viewModel.state.value.pageCache.containsKey(6))
    }

    @Test
    fun `rendering should preserve aspect ratio`() = runTest {
        val uri = "uri1"
        val mockDoc = PdfDocument(uri, 10)
        val openedDoc = OpenedDocument(mockDoc, PagePosition(0, 1.0f), ReadingDirection.LTR)
        
        whenever(openDocumentUseCase(any())) doReturn openedDoc
        // Mock a portrait PDF (100x200)
        whenever(getPageSizeUseCase(any(), any())) doReturn (100 to 200)
        whenever(getPageImageUseCase(any(), any(), any(), any())) doReturn mock<Bitmap>()
        whenever(appConfigRepository.saveLastUri(any())) doAnswer { }
        whenever(appConfigRepository.saveMode(any())) doAnswer { }
        
        viewModel = ReaderViewModel(
            openDocumentUseCase, 
            saveReadingPositionUseCase, 
            saveReadingDirectionUseCase,
            getPageImageUseCase,
            getPageSizeUseCase,
            appConfigRepository
        )
        
        // Set screen to 1000x1000 square
        viewModel.updateScreenDimensions(1000, 1000)
        
        viewModel.loadDocument(uri)
        advanceUntilIdle()
        
        // For a 100x200 PDF on a 1000x1000 screen:
        // Height limit reached: 1000 / 200 = 5x scale.
        // Target Width = 100 * 5 = 500.
        // Target Height = 200 * 5 = 1000.
        verify(getPageImageUseCase).invoke(eq(uri), eq(0), eq(500), eq(1000))
    }

    @Test
    fun `loadDocument should set isLoading to false after successful load`() = runTest {
        val uri = "uri1"
        val mockDoc = PdfDocument(uri, 10)
        val openedDoc = OpenedDocument(mockDoc, PagePosition(0, 1.0f), ReadingDirection.LTR)
        val mockBitmap: Bitmap = mock()
        
        whenever(openDocumentUseCase(any())) doReturn openedDoc
        whenever(getPageSizeUseCase(any(), any())) doReturn (100 to 100)
        whenever(getPageImageUseCase(any(), any(), any(), any())) doReturn mockBitmap
        whenever(appConfigRepository.saveLastUri(any())) doAnswer { }
        whenever(appConfigRepository.saveMode(any())) doAnswer { }
        
        viewModel = ReaderViewModel(
            openDocumentUseCase, 
            saveReadingPositionUseCase, 
            saveReadingDirectionUseCase,
            getPageImageUseCase,
            getPageSizeUseCase,
            appConfigRepository
        )
        
        viewModel.loadDocument(uri)
        advanceUntilIdle()
        
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.errorMessage)
    }
}
