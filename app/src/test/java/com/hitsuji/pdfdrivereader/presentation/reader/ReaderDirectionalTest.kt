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
class ReaderDirectionalTest {

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
    fun `viewModel should correctly switch between all reading directions`() = runTest {
        val uri = "direction_test"
        val mockDoc = PdfDocument(uri, "test.pdf", 10)
        val openedDoc = OpenedDocument(mockDoc, PagePosition(0, 1.0f), ReadingDirection.LTR)
        
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
            appConfigRepository,
            mock()
        )

        viewModel.loadDocument(uri)
        advanceUntilIdle()

        // Test LTR (Initial)
        assertEquals(ReadingDirection.LTR, viewModel.state.value.direction)

        // Switch to RTL
        viewModel.onDirectionChanged(ReadingDirection.RTL)
        advanceUntilIdle()
        assertEquals(ReadingDirection.RTL, viewModel.state.value.direction)
        verify(saveReadingDirectionUseCase).invoke(eq(uri), eq(ReadingDirection.RTL))

        // Switch to TTB
        viewModel.onDirectionChanged(ReadingDirection.TTB)
        advanceUntilIdle()
        assertEquals(ReadingDirection.TTB, viewModel.state.value.direction)
        verify(saveReadingDirectionUseCase).invoke(eq(uri), eq(ReadingDirection.TTB))
    }
}
