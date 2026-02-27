package com.hitsuji.pdfdrivereader.presentation.reader

import android.graphics.Bitmap
import com.hitsuji.pdfdrivereader.domain.model.PagePosition
import com.hitsuji.pdfdrivereader.domain.model.PdfDocument
import com.hitsuji.pdfdrivereader.domain.model.ReadingDirection
import com.hitsuji.pdfdrivereader.domain.repository.AppConfigurationRepository
import com.hitsuji.pdfdrivereader.domain.usecase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@FlowPreview
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReaderContinuousModeTest {

    private val openDocumentUseCase: OpenDocumentUseCase = mock()
    private val saveReadingPositionUseCase: SaveReadingPositionUseCase = mock()
    private val saveReadingDirectionUseCase: SaveReadingDirectionUseCase = mock()
    private val getPageImageUseCase: GetPageImageUseCase = mock()
    private val getPageSizeUseCase: GetPageSizeUseCase = mock()
    private val closeDocumentUseCase: CloseDocumentUseCase = mock()
    private val appConfigRepository: AppConfigurationRepository = mock()
    private lateinit var viewModel: ReaderViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `continuous mode should maintain neighbor pages in cache`() = runTest {
        val uri = "test_uri"
        val mockDoc = PdfDocument(uri, "test.pdf", 100)
        val openedDoc = OpenedDocument(mockDoc, PagePosition(50, 1.0f), ReadingDirection.LTR)
        val mockBitmap: Bitmap = mock()

        whenever(openDocumentUseCase(any())) doReturn openedDoc
        whenever(getPageSizeUseCase(any(), any())) doReturn (1000 to 1000)
        whenever(getPageImageUseCase(any(), any(), any(), any())) doReturn mockBitmap

        viewModel = ReaderViewModel(
            openDocumentUseCase,
            saveReadingPositionUseCase,
            saveReadingDirectionUseCase,
            getPageImageUseCase,
            getPageSizeUseCase,
            closeDocumentUseCase,
            appConfigRepository
        )

        viewModel.loadDocument(uri)
        advanceUntilIdle() 

        // Page 50 is current. Neighbors 48, 49, 51, 52 should be in cache for concatenation.
        val cache = viewModel.state.value.pageCache
        assertEquals("Current page should be cached", true, cache.containsKey(50))
        assertEquals("Previous page should be cached for concatenation", true, cache.containsKey(49))
        assertEquals("Next page should be cached for concatenation", true, cache.containsKey(51))
        assertEquals("Lookahead page should be cached", true, cache.containsKey(52))
        assertEquals("Lookbehind page should be cached", true, cache.containsKey(48))
    }

    @Test
    fun `zoom change should trigger re-rendering for all visible neighbor pages`() = runTest {
        val uri = "test_uri"
        val mockDoc = PdfDocument(uri, "test.pdf", 100)
        val openedDoc = OpenedDocument(mockDoc, PagePosition(10, 1.0f), ReadingDirection.TTB)
        val mockBitmap: Bitmap = mock()

        whenever(openDocumentUseCase(any())) doReturn openedDoc
        whenever(getPageSizeUseCase(any(), any())) doReturn (1000 to 1000)
        whenever(getPageImageUseCase(any(), any(), any(), any())) doReturn mockBitmap

        viewModel = ReaderViewModel(
            openDocumentUseCase,
            saveReadingPositionUseCase,
            saveReadingDirectionUseCase,
            getPageImageUseCase,
            getPageSizeUseCase,
            closeDocumentUseCase,
            appConfigRepository
        )

        viewModel.loadDocument(uri)
        advanceUntilIdle()

        // Change zoom
        viewModel.onZoomChanged(2.0f)
        advanceUntilIdle()

        // Verify that getPageImageUseCase was called for the new zoom level for current and neighbors
        // Initial load: 8, 9, 10, 11, 12 (5 calls)
        // Zoom change: 8, 9, 10, 11, 12 (5 more calls)
        verify(getPageImageUseCase, times(10)).invoke(eq(uri), any(), any(), any())
    }

    @Test
    fun `onZoomChanged should NOT clear cache but update zoom level`() = runTest {
        val uri = "test_uri"
        val mockDoc = PdfDocument(uri, "test.pdf", 10)
        val openedDoc = OpenedDocument(mockDoc, PagePosition(0, 1.0f), ReadingDirection.LTR)
        val mockBitmap: Bitmap = mock()

        whenever(openDocumentUseCase(any())) doReturn openedDoc
        whenever(getPageSizeUseCase(any(), any())) doReturn (1000 to 1000)
        whenever(getPageImageUseCase(any(), any(), any(), any())) doReturn mockBitmap

        viewModel = ReaderViewModel(
            openDocumentUseCase,
            saveReadingPositionUseCase,
            saveReadingDirectionUseCase,
            getPageImageUseCase,
            getPageSizeUseCase,
            closeDocumentUseCase,
            appConfigRepository
        )

        viewModel.loadDocument(uri)
        advanceUntilIdle()

        // Cache should have pages
        assertEquals("Cache should have initial pages", true, viewModel.state.value.pageCache.isNotEmpty())

        viewModel.onZoomChanged(2.5f)
        // Note: We don't advanceUntilIdle yet because we want to check immediately after the call
        runCurrent() 

        assertEquals(2.5f, viewModel.state.value.zoomLevel)
        assertEquals("Cache should NOT be empty after zoom change (it should keep old bitmaps)", true, viewModel.state.value.pageCache.isNotEmpty())
    }

    @Test
    fun `resetZoom should set zoom level back to 1_0`() = runTest {
        val uri = "test_uri"
        val mockDoc = PdfDocument(uri, "test.pdf", 10)
        val openedDoc = OpenedDocument(mockDoc, PagePosition(0, 2.0f), ReadingDirection.LTR)
        val mockBitmap: Bitmap = mock()

        whenever(openDocumentUseCase(any())) doReturn openedDoc
        whenever(getPageSizeUseCase(any(), any())) doReturn (1000 to 1000)
        whenever(getPageImageUseCase(any(), any(), any(), any())) doReturn mockBitmap

        viewModel = ReaderViewModel(
            openDocumentUseCase,
            saveReadingPositionUseCase,
            saveReadingDirectionUseCase,
            getPageImageUseCase,
            getPageSizeUseCase,
            closeDocumentUseCase,
            appConfigRepository
        )

        viewModel.loadDocument(uri)
        advanceUntilIdle()

        assertEquals(2.0f, viewModel.state.value.zoomLevel)

        viewModel.resetZoom()
        advanceUntilIdle()

        assertEquals(1.0f, viewModel.state.value.zoomLevel)
        // Verify it was re-populated with at least the current page
        assertEquals("Cache should be re-populated after reset", true, viewModel.state.value.pageCache.containsKey(0))
    }

    @Test
    fun `zoom level should remain fixed after fingers leave display`() = runTest {
        val uri = "test_uri"
        val mockDoc = PdfDocument(uri, "test.pdf", 10)
        val openedDoc = OpenedDocument(mockDoc, PagePosition(0, 1.0f), ReadingDirection.LTR)
        val mockBitmap: Bitmap = mock()

        whenever(openDocumentUseCase(any())) doReturn openedDoc
        whenever(getPageSizeUseCase(any(), any())) doReturn (1000 to 1000)
        whenever(getPageImageUseCase(any(), any(), any(), any())) doReturn mockBitmap

        viewModel = ReaderViewModel(
            openDocumentUseCase,
            saveReadingPositionUseCase,
            saveReadingDirectionUseCase,
            getPageImageUseCase,
            getPageSizeUseCase,
            closeDocumentUseCase,
            appConfigRepository
        )

        viewModel.loadDocument(uri)
        advanceUntilIdle()

        // Simulate a zoom action in UI reaching 3.0f
        viewModel.onZoomChanged(3.0f)
        advanceUntilIdle()

        // The zoom in state should be exactly 3.0f
        assertEquals(3.0f, viewModel.state.value.zoomLevel, 0.001f)
        
        // Wait even more to see if anything else triggers a change
        advanceUntilIdle()
        
        assertEquals("Zoom level should not have drifted", 3.0f, viewModel.state.value.zoomLevel, 0.001f)
    }
}
