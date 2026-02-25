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
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
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
        assertTrue(viewModel.state.value.isUiVisible)
        
        viewModel.toggleUI()
        assertFalse(viewModel.state.value.isUiVisible)
    }

    @Test
    fun `onPageChanged should update state and load bitmap`() = runTest {
        val mockBitmap: Bitmap = mock()
        val uri = "uri1"
        val mockDoc = PdfDocument(uri, 10)
        val openedDoc = OpenedDocument(mockDoc, PagePosition(0, 1.0f), ReadingDirection.LTR)
        
        whenever(openDocumentUseCase(any())) doReturn openedDoc
        whenever(getPageImageUseCase(any(), any(), any(), any())) doReturn mockBitmap
        
        viewModel = ReaderViewModel(
            openDocumentUseCase, 
            saveReadingPositionUseCase, 
            saveReadingDirectionUseCase,
            getPageImageUseCase,
            appConfigRepository
        )
        
        // Setup document in state
        viewModel.loadDocument(uri)
        advanceUntilIdle()
        
        viewModel.onPageChanged(5)
        advanceUntilIdle()
        
        assertEquals(5, viewModel.state.value.currentPage)
        // Verify bitmap was loaded for page 5
        verify(getPageImageUseCase, atLeastOnce()).invoke(eq(uri), eq(5), any(), any())
    }
}
