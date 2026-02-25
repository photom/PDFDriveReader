package com.hitsuji.pdfdrivereader.presentation.reader

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val openedDoc = openDocumentUseCase(uri)
                _state.update { 
                    it.copy(
                        isLoading = false,
                        document = openedDoc.document,
                        currentPage = openedDoc.position.pageIndex,
                        zoomLevel = openedDoc.position.zoomLevel,
                        direction = openedDoc.direction
                    )
                }
                // Initial page load
                loadPageBitmap(uri, openedDoc.position.pageIndex)
                
                // Persist session
                appConfigRepository.saveLastUri(uri)
            } catch (e: Exception) {
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
        // Use a default size for now, ideally this comes from UI measurement
        val bitmap = getPageImageUseCase(uri, index, 1080, 1920) as Bitmap
        _state.update { it.copy(currentPageBitmap = bitmap) }
    }

    /**
     * Toggles the visibility of the menu overlay.
     */
    fun toggleUI() {
        _state.update { it.copy(isUiVisible = !it.isUiVisible) }
    }

    /**
     * Updates the current page index and persists it to storage.
     * 
     * @param index The 0-based page index.
     */
    fun onPageChanged(index: Int) {
        val documentId = _state.value.document?.id ?: return
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
        _state.update { it.copy(direction = direction) }
        val documentId = _state.value.document?.id ?: return
        viewModelScope.launch {
            saveReadingDirectionUseCase(documentId, direction)
        }
    }
}
