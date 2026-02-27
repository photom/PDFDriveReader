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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.min

/**
 * ViewModel for the immersive Reader screen.
 */
@kotlinx.coroutines.FlowPreview
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
    private var lastRefreshZoom: Float = 1.0f
    
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
                performRefreshCache()
                
            } catch (e: Exception) {
                Log.e("PDFDriveReader", "Reader: Fatal error opening document", e)
                _state.update { it.copy(errorMessage = e.message ?: "Failed to open document") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun performRefreshCache() {
        val uri = _state.value.document?.id ?: return
        val centerIndex = _state.value.currentPage
        val currentZoom = _state.value.zoomLevel
        val forceRefresh = currentZoom != lastRefreshZoom
        lastRefreshZoom = currentZoom
        
        cacheJob?.cancel()
        cacheJob = viewModelScope.launch {
            val totalPages = _state.value.document?.totalPageCount ?: return@launch
            
            // Priority 1: Current page
            if (forceRefresh || !_state.value.pageCache.containsKey(centerIndex)) {
                loadPageIntoCache(uri, centerIndex)
            }

            // Priority 2: Neighbor pages for concatenation
            val indicesToLoad = listOf(centerIndex - 2, centerIndex - 1, centerIndex + 1, centerIndex + 2)
                .filter { it in 0 until totalPages }

            indicesToLoad.forEach { index ->
                if (forceRefresh || !_state.value.pageCache.containsKey(index)) {
                    loadPageIntoCache(uri, index)
                }
            }

            // Cleanup: Keep only what's in the window
            val fullWindow = (indicesToLoad + centerIndex).toSet()
            _state.update { currentState ->
                currentState.copy(pageCache = currentState.pageCache.filterKeys { it in fullWindow })
            }
        }
    }

    private suspend fun loadPageIntoCache(uri: String, index: Int) {
        try {
            val originalSize = getPageSizeUseCase(uri, index)
            val pdfWidth = originalSize.first
            val pdfHeight = originalSize.second
            
            val zoom = _state.value.zoomLevel
            val scale = min(screenWidth.toFloat() / pdfWidth, screenHeight.toFloat() / pdfHeight) * zoom
            val targetWidth = (pdfWidth * scale).toInt()
            val targetHeight = (pdfHeight * scale).toInt()

            Log.d("PDFDriveReader", "Reader: Requesting bitmap for page $index at zoom $zoom")
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
        performRefreshCache()
        
        viewModelScope.launch {
            saveReadingPositionUseCase(documentId, PagePosition(index, _state.value.zoomLevel))
        }
    }

    fun onZoomChanged(zoom: Float) {
        if (zoom == _state.value.zoomLevel) return
        val documentId = _state.value.document?.id ?: return
        
        _state.update { it.copy(zoomLevel = zoom) }
        performRefreshCache()
    }

    fun resetZoom() {
        if (_state.value.zoomLevel == 1.0f) return
        val documentId = _state.value.document?.id ?: return
        
        _state.update { it.copy(zoomLevel = 1.0f) }
        performRefreshCache()
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
