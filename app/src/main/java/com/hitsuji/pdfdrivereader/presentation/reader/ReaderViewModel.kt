package com.hitsuji.pdfdrivereader.presentation.reader

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitsuji.pdfdrivereader.domain.model.AppMode
import com.hitsuji.pdfdrivereader.domain.model.PagePosition
import com.hitsuji.pdfdrivereader.domain.model.ReadingDirection
import com.hitsuji.pdfdrivereader.domain.repository.AppConfigurationRepository
import com.hitsuji.pdfdrivereader.domain.usecase.GetPageImageUseCase
import com.hitsuji.pdfdrivereader.domain.usecase.OpenDocumentUseCase
import com.hitsuji.pdfdrivereader.domain.usecase.SaveReadingDirectionUseCase
import com.hitsuji.pdfdrivereader.domain.usecase.SaveReadingPositionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the immersive Reader screen.
 */
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val openDocumentUseCase: OpenDocumentUseCase,
    private val saveReadingPositionUseCase: SaveReadingPositionUseCase,
    private val saveReadingDirectionUseCase: SaveReadingDirectionUseCase,
    private val getPageImageUseCase: GetPageImageUseCase,
    private val appConfigRepository: AppConfigurationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderState())
    /**
     * Observable [StateFlow] representing the reader's UI state.
     */
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    /**
     * Loads a PDF document and restores its last saved state.
     * 
     * @param uri The document unique identifier.
     */
    fun loadDocument(uri: String) {
        Log.d("PDFDriveReader", "Reader: Loading document $uri")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val openedDoc = openDocumentUseCase(uri)
                Log.d("PDFDriveReader", "Reader: Document opened. Count=${openedDoc.document.totalPageCount}, Direction=${openedDoc.direction}")
                
                _state.update { 
                    it.copy(
                        document = openedDoc.document,
                        currentPage = openedDoc.position.pageIndex,
                        zoomLevel = openedDoc.position.zoomLevel,
                        direction = openedDoc.direction
                    )
                }
                
                // Initial page load
                loadPageBitmap(uri, openedDoc.position.pageIndex)
                
                // Persist session
                Log.d("PDFDriveReader", "Reader: Saving session mode=READER, uri=$uri")
                appConfigRepository.saveMode(AppMode.READER)
                appConfigRepository.saveLastUri(uri)
                
                _state.update { it.copy(isLoading = false) }
                
            } catch (e: Exception) {
                Log.e("PDFDriveReader", "Reader: Failed to open document $uri", e)
                _state.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to open document"
                    )
                }
            }
        }
    }

    /**
     * Loads the bitmap for a specific page.
     */
    private suspend fun loadPageBitmap(uri: String, index: Int) {
        try {
            Log.d("PDFDriveReader", "Reader: Loading bitmap for page $index")
            val bitmap = getPageImageUseCase(uri, index, 1080, 1920) as Bitmap
            _state.update { it.copy(currentPageBitmap = bitmap) }
            Log.d("PDFDriveReader", "Reader: Bitmap loaded for page $index")
        } catch (e: Exception) {
            Log.e("PDFDriveReader", "Reader: Failed to render page $index", e)
            _state.update { it.copy(errorMessage = "Error rendering page $index") }
        }
    }

    /**
     * Toggles the visibility of the menu overlay.
     */
    fun toggleUI() {
        Log.d("PDFDriveReader", "Reader: Toggle UI")
        _state.update { it.copy(isUiVisible = !it.isUiVisible) }
    }

    /**
     * Updates the current page index and persists it to storage.
     * 
     * @param index The 0-based page index.
     */
    fun onPageChanged(index: Int) {
        val documentId = _state.value.document?.id ?: return
        if (index == _state.value.currentPage && _state.value.currentPageBitmap != null) return

        Log.d("PDFDriveReader", "Reader: Page changed to $index")
        _state.update { it.copy(currentPage = index) }
        viewModelScope.launch {
            loadPageBitmap(documentId, index)
            saveReadingPositionUseCase(
                documentId, 
                PagePosition(index, _state.value.zoomLevel)
            )
        }
    }

    /**
     * Updates the reading direction and persists it.
     * 
     * @param direction The new [ReadingDirection].
     */
    fun onDirectionChanged(direction: ReadingDirection) {
        Log.d("PDFDriveReader", "Reader: Direction changed to $direction")
        _state.update { it.copy(direction = direction) }
        val documentId = _state.value.document?.id ?: return
        viewModelScope.launch {
            saveReadingDirectionUseCase(documentId, direction)
        }
    }
}
