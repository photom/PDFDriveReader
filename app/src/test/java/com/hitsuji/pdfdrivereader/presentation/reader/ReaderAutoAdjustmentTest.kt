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
class ReaderAutoAdjustmentTest {

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
    fun `onZoomChanged should NOT change the current page index`() = runTest {
        val uri = "adjustment_test"
        val mockDoc = PdfDocument(uri, "test.pdf", 100)
        val openedDoc = OpenedDocument(mockDoc, PagePosition(50, 1.0f), ReadingDirection.TTB)
        
        whenever(openDocumentUseCase(any())) doReturn openedDoc
        whenever(getPageSizeUseCase(any(), any())) doReturn (1000 to 1000)
        whenever(getPageImageUseCase(any(), any(), any(), any())) doReturn mock<Bitmap>()

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

        assertEquals(50, viewModel.state.value.currentPage)

        // Zoom in
        viewModel.onZoomChanged(3.0f)
        advanceUntilIdle()

        // Page should still be 50, and NO internal logic should have tried to "correct" it to 0 or similar
        assertEquals(50, viewModel.state.value.currentPage)
        assertEquals(3.0f, viewModel.state.value.zoomLevel)
    }
}
