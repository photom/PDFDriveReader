package com.hitsuji.pdfdrivereader.presentation.reader

import android.graphics.Bitmap
import com.hitsuji.pdfdrivereader.domain.usecase.*
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
    private val closeDocumentUseCase: CloseDocumentUseCase = mock()
    private val appConfigRepository: AppConfigurationRepository = mock()
    private val getTextSelectionUseCase: GetTextSelectionUseCase = mock()
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
            closeDocumentUseCase,
            appConfigRepository,
            getTextSelectionUseCase
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
        val mockDoc = PdfDocument(uri, "doc.pdf", 10)
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
            closeDocumentUseCase,
            appConfigRepository,
            getTextSelectionUseCase
        )        
        viewModel.loadDocument(uri)
        advanceUntilIdle()
        
        viewModel.onPageChanged(5)
        advanceUntilIdle()
        
        assertEquals(5, viewModel.state.value.currentPage)
        // Verify multiple pages around 5 were loaded (3, 4, 5, 6, 7)
        assertTrue(viewModel.state.value.pageCache.containsKey(3))
        assertTrue(viewModel.state.value.pageCache.containsKey(4))
        assertTrue(viewModel.state.value.pageCache.containsKey(5))
        assertTrue(viewModel.state.value.pageCache.containsKey(6))
        assertTrue(viewModel.state.value.pageCache.containsKey(7))
    }

    @Test
    fun `rapid onPageChanged should cancel previous caching jobs`() = runTest {
        val uri = "uri1"
        val mockDoc = PdfDocument(uri, "doc.pdf", 100)
        val openedDoc = OpenedDocument(mockDoc, PagePosition(0, 1.0f), ReadingDirection.LTR)
        
        whenever(openDocumentUseCase(any())) doReturn openedDoc
        whenever(getPageSizeUseCase(any(), any())) doReturn (100 to 100)
        whenever(getPageImageUseCase(any(), any(), any(), any())) doAnswer {
            mock<Bitmap>()
        }
        whenever(appConfigRepository.saveLastUri(any())) doAnswer { }
        whenever(appConfigRepository.saveMode(any())) doAnswer { }
        
        viewModel = ReaderViewModel(
            openDocumentUseCase,
            saveReadingPositionUseCase,
            saveReadingDirectionUseCase,
            getPageImageUseCase,
            getPageSizeUseCase,
            closeDocumentUseCase,
            appConfigRepository,
            getTextSelectionUseCase
        )        
        viewModel.loadDocument(uri)
        advanceUntilIdle()
        
        viewModel.onPageChanged(10)
        viewModel.onPageChanged(20)
        viewModel.onPageChanged(30)
        
        advanceUntilIdle()
        
        assertEquals(30, viewModel.state.value.currentPage)
        // Window should be 28, 29, 30, 31, 32
        assertTrue(viewModel.state.value.pageCache.containsKey(28))
        assertTrue(viewModel.state.value.pageCache.containsKey(29))
        assertTrue(viewModel.state.value.pageCache.containsKey(30))
        assertTrue(viewModel.state.value.pageCache.containsKey(31))
        assertTrue(viewModel.state.value.pageCache.containsKey(32))
        // Old pages (e.g., 10) should have been purged or never fully cached
        assertFalse(viewModel.state.value.pageCache.containsKey(10))
    }

    @Test
    fun `onDirectionChanged should update state and persist`() = runTest {
        val uri = "uri1"
        val mockDoc = PdfDocument(uri, "doc.pdf", 10)
        val openedDoc = OpenedDocument(mockDoc, PagePosition(0, 1.0f), ReadingDirection.LTR)
        
        whenever(openDocumentUseCase(any())) doReturn openedDoc
        whenever(appConfigRepository.saveLastUri(any())) doAnswer { }
        whenever(appConfigRepository.saveMode(any())) doAnswer { }
        
        viewModel = ReaderViewModel(
            openDocumentUseCase,
            saveReadingPositionUseCase,
            saveReadingDirectionUseCase,
            getPageImageUseCase,
            getPageSizeUseCase,
            closeDocumentUseCase,
            appConfigRepository,
            getTextSelectionUseCase
        )        
        viewModel.loadDocument(uri)
        advanceUntilIdle()
        
        viewModel.onDirectionChanged(ReadingDirection.RTL)
        advanceUntilIdle()
        
        assertEquals(ReadingDirection.RTL, viewModel.state.value.direction)
        verify(saveReadingDirectionUseCase).invoke(eq(uri), eq(ReadingDirection.RTL))
    }

    @Test
    fun `onZoomChanged should update state and refresh cache`() = runTest {
        val uri = "uri1"
        val mockDoc = PdfDocument(uri, "doc.pdf", 10)
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
            closeDocumentUseCase,
            appConfigRepository,
            getTextSelectionUseCase
        )
        viewModel.loadDocument(uri)
        advanceUntilIdle()

        viewModel.onZoomChanged(2.0f)
        advanceUntilIdle()

        assertEquals(2.0f, viewModel.state.value.zoomLevel)
        // Verify rendering was requested again for the new zoom level
        verify(getPageImageUseCase, atLeast(2)).invoke(eq(uri), eq(0), any(), any())
    }

    @Test
    fun `loadDocument should set isLoading to false after successful load`() = runTest {
        val uri = "uri1"
        val mockDoc = PdfDocument(uri, "doc.pdf", 10)
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
            closeDocumentUseCase,
            appConfigRepository,
            getTextSelectionUseCase
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
            getPageSizeUseCase,
            closeDocumentUseCase,
            appConfigRepository,
            getTextSelectionUseCase
        )        
        viewModel.loadDocument(uri)
        advanceUntilIdle()
        
        assertFalse(viewModel.state.value.isLoading)
        assertEquals("IO Error", viewModel.state.value.errorMessage)
    }

    @Test
    fun `selectTextAt should update textSelection state when usecase returns data`() = runTest {
        val uri = "uri1"
        val openedDoc = com.hitsuji.pdfdrivereader.domain.usecase.OpenedDocument(PdfDocument(uri, "file1.pdf", 5, listOf()), PagePosition(0, 1.0f), ReadingDirection.LTR)
        whenever(openDocumentUseCase(any())) doReturn openedDoc
        whenever(getPageSizeUseCase(any(), any())) doReturn Pair(100, 100)
        whenever(getPageImageUseCase(any(), any(), any(), any())) doReturn mock()
        whenever(appConfigRepository.saveLastUri(any())) doAnswer { }
        whenever(appConfigRepository.saveMode(any())) doAnswer { }
        
        val selection = com.hitsuji.pdfdrivereader.domain.model.PdfTextSelection(0, "Selected Text", emptyList(), android.graphics.PointF(10f, 20f), android.graphics.PointF(10f, 20f))
        whenever(getTextSelectionUseCase(eq(uri), eq(0), eq(10), eq(20), eq(10), eq(20))) doReturn selection
        
        viewModel = ReaderViewModel(
            openDocumentUseCase,
            saveReadingPositionUseCase,
            saveReadingDirectionUseCase,
            getPageImageUseCase,
            getPageSizeUseCase,
            closeDocumentUseCase,
            appConfigRepository,
            getTextSelectionUseCase
        )
        
        viewModel.loadDocument(uri)
        advanceUntilIdle()
        
        assertNull(viewModel.state.value.textSelection)
        
        viewModel.selectTextAt(0, 10, 20)
        advanceUntilIdle()
        
        assertEquals(selection, viewModel.state.value.textSelection)
        
        viewModel.clearSelection()
        advanceUntilIdle()
        assertNull(viewModel.state.value.textSelection)
    }

    @Test
    fun `updateSelectionStart and updateSelectionStop should update state`() = runTest {
        val uri = "uri1"
        val openedDoc = com.hitsuji.pdfdrivereader.domain.usecase.OpenedDocument(PdfDocument(uri, "file1.pdf", 5, listOf()), PagePosition(0, 1.0f), ReadingDirection.LTR)
        whenever(openDocumentUseCase(any())) doReturn openedDoc
        whenever(getPageSizeUseCase(any(), any())) doReturn Pair(100, 100)
        whenever(getPageImageUseCase(any(), any(), any(), any())) doReturn mock()
        whenever(appConfigRepository.saveLastUri(any())) doAnswer { }
        whenever(appConfigRepository.saveMode(any())) doAnswer { }
        
        val initialSelection = com.hitsuji.pdfdrivereader.domain.model.PdfTextSelection(0, "Word", listOf(android.graphics.RectF(10f, 10f, 30f, 20f)), android.graphics.PointF(20f, 15f), android.graphics.PointF(20f, 15f))
        whenever(getTextSelectionUseCase(eq(uri), eq(0), eq(20), eq(15), eq(20), eq(15))) doReturn initialSelection
        
        viewModel = ReaderViewModel(
            openDocumentUseCase,
            saveReadingPositionUseCase,
            saveReadingDirectionUseCase,
            getPageImageUseCase,
            getPageSizeUseCase,
            closeDocumentUseCase,
            appConfigRepository,
            getTextSelectionUseCase
        )
        
        viewModel.loadDocument(uri)
        advanceUntilIdle()
        
        viewModel.selectTextAt(0, 20, 15)
        advanceUntilIdle()
        assertEquals(initialSelection, viewModel.state.value.textSelection)
        
        val newStartSelection = com.hitsuji.pdfdrivereader.domain.model.PdfTextSelection(0, "Start Word", listOf(android.graphics.RectF(5f, 10f, 30f, 20f)), android.graphics.PointF(5f, 15f), android.graphics.PointF(20f, 15f))
        whenever(getTextSelectionUseCase(eq(uri), eq(0), eq(5), eq(15), eq(20), eq(15))) doReturn newStartSelection
        
        viewModel.updateSelectionStart(0, 5, 15)
        advanceUntilIdle()
        assertEquals(newStartSelection, viewModel.state.value.textSelection)
        
        val newStopSelection = com.hitsuji.pdfdrivereader.domain.model.PdfTextSelection(0, "Start Word End", listOf(android.graphics.RectF(5f, 10f, 40f, 20f)), android.graphics.PointF(5f, 15f), android.graphics.PointF(40f, 15f))
        whenever(getTextSelectionUseCase(eq(uri), eq(0), eq(5), eq(15), eq(40), eq(15))) doReturn newStopSelection
        
        viewModel.updateSelectionStop(0, 40, 15)
        advanceUntilIdle()
        assertEquals(newStopSelection, viewModel.state.value.textSelection)
    }

    @Test
    fun `onDocumentTapped should clear selection if tapped outside bounds, keep if inside, or toggle UI if no selection`() = runTest {
        val uri = "uri1"
        val openedDoc = com.hitsuji.pdfdrivereader.domain.usecase.OpenedDocument(PdfDocument(uri, "file1.pdf", 5, listOf()), PagePosition(0, 1.0f), ReadingDirection.LTR)
        whenever(openDocumentUseCase(any())) doReturn openedDoc
        whenever(getPageSizeUseCase(any(), any())) doReturn Pair(100, 100)
        whenever(getPageImageUseCase(any(), any(), any(), any())) doReturn mock()
        whenever(appConfigRepository.saveLastUri(any())) doAnswer { }
        whenever(appConfigRepository.saveMode(any())) doAnswer { }
        
        val selection = com.hitsuji.pdfdrivereader.domain.model.PdfTextSelection(0, "Word", listOf(android.graphics.RectF(10f, 10f, 30f, 20f)), android.graphics.PointF(15f, 15f), android.graphics.PointF(15f, 15f))
        whenever(getTextSelectionUseCase(eq(uri), eq(0), eq(15), eq(15), eq(15), eq(15))) doReturn selection
        
        viewModel = ReaderViewModel(
            openDocumentUseCase,
            saveReadingPositionUseCase,
            saveReadingDirectionUseCase,
            getPageImageUseCase,
            getPageSizeUseCase,
            closeDocumentUseCase,
            appConfigRepository,
            getTextSelectionUseCase
        )
        
        viewModel.loadDocument(uri)
        advanceUntilIdle()
        
        assertFalse(viewModel.state.value.isUiVisible)
        viewModel.onDocumentTapped(5, 5) // No selection yet
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isUiVisible)
        
        viewModel.selectTextAt(0, 15, 15)
        advanceUntilIdle()
        assertEquals(selection, viewModel.state.value.textSelection)
        
        viewModel.onDocumentTapped(15, 15) // Inside bounds
        advanceUntilIdle()
        assertEquals(selection, viewModel.state.value.textSelection)
        assertTrue(viewModel.state.value.isUiVisible) // Still visible
        
        viewModel.onDocumentTapped(5, 5) // Outside bounds
        advanceUntilIdle()
        assertNull(viewModel.state.value.textSelection)
    }
}
