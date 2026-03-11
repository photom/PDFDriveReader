package com.hitsuji.pdfdrivereader.presentation.reader

import android.graphics.Bitmap
import com.hitsuji.pdfdrivereader.domain.model.PagePosition
import com.hitsuji.pdfdrivereader.domain.model.PdfDocument
import com.hitsuji.pdfdrivereader.domain.model.ReadingDirection
import com.hitsuji.pdfdrivereader.domain.repository.AppConfigurationRepository
import com.hitsuji.pdfdrivereader.domain.usecase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReaderPerformanceTest {

    private val openDocumentUseCase: OpenDocumentUseCase = mock()
    private val saveReadingPositionUseCase: SaveReadingPositionUseCase = mock()
    private val saveReadingDirectionUseCase: SaveReadingDirectionUseCase = mock()
    private val saveCoverModeUseCase: SaveCoverModeUseCase = mock()
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
    fun `rapid zoom changes should be debounced`() = runTest {
        val uri = "test_uri"
        val mockDoc = PdfDocument(uri, "test.pdf", 100)
        val openedDoc = OpenedDocument(mockDoc, PagePosition(10, 1.0f), ReadingDirection.LTR, true)
        val mockBitmap: Bitmap = mock()

        whenever(openDocumentUseCase(any())) doReturn openedDoc
        whenever(getPageSizeUseCase(any(), any())) doReturn (1000 to 1000)
        whenever(getPageImageUseCase(any(), any(), any(), any())) doReturn mockBitmap

        viewModel = ReaderViewModel(
            openDocumentUseCase,
            saveReadingPositionUseCase,
            saveReadingDirectionUseCase,
            saveCoverModeUseCase,
            getPageImageUseCase,
            getPageSizeUseCase,
            closeDocumentUseCase,
            appConfigRepository,
            mock()
        )

        viewModel.loadDocument(uri)
        advanceUntilIdle()

        // Rapidly change zoom 5 times
        repeat(5) { i ->
            viewModel.onZoomChanged(1.1f + i * 0.1f)
            // No advanceUntilIdle here, simulating rapid input
        }

        // Wait for debounce (150ms in code, so we wait slightly more)
        advanceTimeBy(200)
        runCurrent()
        advanceUntilIdle()

        // Verify that getPageImageUseCase was only called once for the FINAL zoom level (plus initial load)
        // Initial load: 5 calls (8, 9, 10, 11, 12)
        // Debounced zoom change: 5 calls (8, 9, 10, 11, 12)
        // Total should be around 10. We use atLeast(6) and atMost(15) to be safe but prove it didn't call 30+ times.
        verify(getPageImageUseCase, atLeast(6)).invoke(eq(uri), any(), any(), any())
        verify(getPageImageUseCase, atMost(15)).invoke(eq(uri), any(), any(), any())
        assertEquals(1.5f, viewModel.state.value.zoomLevel, 0.01f)
    }

    @Test
    fun `refresh should prioritize current page`() = runTest {
        val uri = "test_uri"
        val mockDoc = PdfDocument(uri, "test.pdf", 100)
        val openedDoc = OpenedDocument(mockDoc, PagePosition(10, 1.0f), ReadingDirection.LTR, true)
        
        whenever(openDocumentUseCase(any())) doReturn openedDoc
        whenever(getPageSizeUseCase(any(), any())) doReturn (1000 to 1000)
        
        // Use a slow render mock to test prioritization
        whenever(getPageImageUseCase(any(), any(), any(), any())).thenAnswer {
            Thread.sleep(10) // Simulate slow work
            mock<Bitmap>()
        }

        viewModel = ReaderViewModel(
            openDocumentUseCase,
            saveReadingPositionUseCase,
            saveReadingDirectionUseCase,
            saveCoverModeUseCase,
            getPageImageUseCase,
            getPageSizeUseCase,
            closeDocumentUseCase,
            appConfigRepository,
            mock()
        )

        viewModel.loadDocument(uri)
        advanceUntilIdle()

        // Reset mocks to track order
        clearInvocations(getPageImageUseCase)

        viewModel.onPageChanged(20)
        advanceTimeBy(200) // Trigger debounce
        runCurrent()
        
        // We want to verify that page 20 is requested first
        val inOrder = inOrder(getPageImageUseCase)
        advanceUntilIdle()
        
        inOrder.verify(getPageImageUseCase).invoke(eq(uri), eq(20), any(), any())
        inOrder.verify(getPageImageUseCase, atLeastOnce()).invoke(eq(uri), any(), any(), any())
    }
}
