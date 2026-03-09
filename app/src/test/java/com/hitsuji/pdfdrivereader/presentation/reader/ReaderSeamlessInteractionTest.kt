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
class ReaderSeamlessInteractionTest {

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
    fun `viewport zoom should affect all pages in state`() = runTest {
        val uri = "seamless_test"
        val mockDoc = PdfDocument(uri, "seamless.pdf", 10)
        val openedDoc = OpenedDocument(mockDoc, PagePosition(0, 1.0f), ReadingDirection.LTR, true)
        
        whenever(openDocumentUseCase(any())) doReturn openedDoc
        whenever(getPageSizeUseCase(any(), any())) doReturn (1000 to 1000)
        whenever(getPageImageUseCase(any(), any(), any(), any())) doReturn mock<Bitmap>()

        viewModel = ReaderViewModel(
            openDocumentUseCase,
            saveReadingPositionUseCase,
            saveReadingDirectionUseCase,
            saveCoverModeUseCase,
            getPageImageUseCase,
            getPageSizeUseCase,
            closeDocumentUseCase,
            appConfigRepository
        )

        viewModel.loadDocument(uri)
        advanceUntilIdle()

        // Simulate viewport-level zoom change
        viewModel.onZoomChanged(2.0f)
        advanceUntilIdle()

        // State zoom level should be updated
        assertEquals(2.0f, viewModel.state.value.zoomLevel)
        
        // Cache should be updated for the window with the new zoom
        // (Previously verified, but important here too)
        assertEquals(true, viewModel.state.value.pageCache.isNotEmpty())
    }
}
