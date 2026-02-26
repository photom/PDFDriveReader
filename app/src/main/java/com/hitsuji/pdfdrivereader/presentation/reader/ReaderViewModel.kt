package com.hitsuji.pdfdrivereader.presentation.reader

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitsuji.pdfdrivereader.domain.model.AppMode
import com.hitsuji.pdfdrivereader.domain.model.PagePosition
import com.hitsuji.pdfdrivereader.domain.model.ReadingDirection
import com.hitsuji.pdfdrivereader.domain.repository.AppConfigurationRepository
import com.hitsuji.pdfdrivereader.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.min

/**
 * ViewModel for the immersive Reader screen.
 */
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val openDocumentUseCase: OpenDocumentUseCase,
    private val saveReadingPositionUseCase: SaveReadingPositionUseCase,
    private val saveReadingDirectionUseCase: SaveReadingDirectionUseCase,
    private val getPageImageUseCase: GetPageImageUseCase,
    private val getPageSizeUseCase: GetPageSizeUseCase,
    private val closeDocumentUseCase: CloseDocumentUseCase,
    private val appConfigRepository: AppConfigurationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderState())
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    private var cacheJob: Job? = null

    private var screenWidth: Int = 1080
    private var screenHeight: Int = 1920

    fun updateScreenDimensions(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }

    fun loadDocument(uri: String) {
        Log.d("PDFDriveReader", "Reader: loadDocument triggered for $uri")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, pageCache = emptyMap()) }
            try {
                val openedDoc = openDocumentUseCase(uri)
                _state.update { 
                    it.copy(
                        document = openedDoc.document,
                        currentPage = openedDoc.position.pageIndex,
                        zoomLevel = openedDoc.position.zoomLevel,
                        direction = openedDoc.direction
                    )
                }
                
                loadPageIntoCache(uri, openedDoc.position.pageIndex)
                appConfigRepository.saveLastUri(uri)
                appConfigRepository.saveMode(AppMode.READER)
                refreshCache(uri, openedDoc.position.pageIndex)
                
            } catch (e: Exception) {
                Log.e("PDFDriveReader", "Reader: Fatal error opening document", e)
                _state.update { it.copy(errorMessage = e.message ?: "Failed to open document") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun refreshCache(uri: String, centerIndex: Int) {
        cacheJob?.cancel()
        cacheJob = viewModelScope.launch {
            val totalPages = _state.value.document?.totalPageCount ?: return@launch
            // Cache window: 2 pages before, 2 pages after
            val indicesToLoad = listOf(centerIndex - 2, centerIndex - 1, centerIndex, centerIndex + 1, centerIndex + 2)
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
    }

    private suspend fun loadPageIntoCache(uri: String, index: Int) {
        try {
            val originalSize = getPageSizeUseCase(uri, index)
            val pdfWidth = originalSize.first
            val pdfHeight = originalSize.second
            
            val scale = min(screenWidth.toFloat() / pdfWidth, screenHeight.toFloat() / pdfHeight)
            val targetWidth = (pdfWidth * scale).toInt()
            val targetHeight = (pdfHeight * scale).toInt()

            Log.d("PDFDriveReader", "Reader: Requesting bitmap for page $index")
            val bitmap = getPageImageUseCase(uri, index, targetWidth, targetHeight) as Bitmap
            
            _state.update { currentState ->
                val newCache = currentState.pageCache.toMutableMap().apply { put(index, bitmap) }
                currentState.copy(pageCache = newCache)
            }
        } catch (e: Exception) {
            if (e !is kotlinx.coroutines.CancellationException) {
                Log.e("PDFDriveReader", "Reader: Failed to cache page $index", e)
            }
        }
    }

    fun toggleUI() {
        _state.update { it.copy(isUiVisible = !it.isUiVisible) }
    }

    fun onPageChanged(index: Int) {
        val documentId = _state.value.document?.id ?: return
        if (index == _state.value.currentPage) return

        _state.update { it.copy(currentPage = index) }
        refreshCache(documentId, index)
        
        viewModelScope.launch {
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

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            Log.d("PDFDriveReader", "Reader: ViewModel cleared, closing document session")
            closeDocumentUseCase()
        }
    }
}
