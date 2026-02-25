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
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    fun loadDocument(uri: String) {
        Log.d("PDFDriveReader", "Reader: loadDocument triggered for $uri")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, pageCache = emptyMap()) }
            try {
                val openedDoc = openDocumentUseCase(uri)
                Log.d("PDFDriveReader", "Reader: Metadata loaded. Pages=${openedDoc.document.totalPageCount}")
                
                _state.update { 
                    it.copy(
                        document = openedDoc.document,
                        currentPage = openedDoc.position.pageIndex,
                        zoomLevel = openedDoc.position.zoomLevel,
                        direction = openedDoc.direction
                    )
                }
                
                // Load the initial page immediately
                loadPageIntoCache(uri, openedDoc.position.pageIndex)
                
                // Persist session
                appConfigRepository.saveMode(AppMode.READER)
                appConfigRepository.saveLastUri(uri)
                
                // Trigger neighbor caching in the background
                launch { refreshPageCache(uri, openedDoc.position.pageIndex) }
                
            } catch (e: Exception) {
                Log.e("PDFDriveReader", "Reader: Fatal error opening document", e)
                _state.update { it.copy(errorMessage = e.message ?: "Failed to open document") }
            } finally {
                Log.d("PDFDriveReader", "Reader: Setting isLoading = false")
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun refreshPageCache(uri: String, centerIndex: Int) {
        val totalPages = _state.value.document?.totalPageCount ?: return
        val indicesToLoad = listOf(centerIndex - 1, centerIndex, centerIndex + 1)
            .filter { it in 0 until totalPages }

        indicesToLoad.forEach { index ->
            if (!_state.value.pageCache.containsKey(index)) {
                loadPageIntoCache(uri, index)
            }
        }

        _state.update { currentState ->
            currentState.copy(pageCache = currentState.pageCache.filterKeys { it in indicesToLoad })
        }
    }

    private suspend fun loadPageIntoCache(uri: String, index: Int) {
        try {
            Log.d("PDFDriveReader", "Reader: Requesting bitmap for page $index")
            val bitmap = getPageImageUseCase(uri, index, 1080, 1920) as Bitmap
            _state.update { currentState ->
                val newCache = currentState.pageCache.toMutableMap().apply { put(index, bitmap) }
                currentState.copy(pageCache = newCache)
            }
            Log.d("PDFDriveReader", "Reader: Page $index cached")
        } catch (e: Exception) {
            Log.e("PDFDriveReader", "Reader: Failed to cache page $index", e)
        }
    }

    fun toggleUI() {
        _state.update { it.copy(isUiVisible = !it.isUiVisible) }
    }

    fun onPageChanged(index: Int) {
        val documentId = _state.value.document?.id ?: return
        if (index == _state.value.currentPage) return

        _state.update { it.copy(currentPage = index) }
        viewModelScope.launch {
            refreshPageCache(documentId, index)
            saveReadingPositionUseCase(documentId, PagePosition(index, _state.value.zoomLevel))
        }
    }

    fun onDirectionChanged(direction: ReadingDirection) {
        _state.update { it.copy(direction = direction) }
        val documentId = _state.value.document?.id ?: return
        viewModelScope.launch {
            saveReadingDirectionUseCase(documentId, direction)
        }
    }
}
