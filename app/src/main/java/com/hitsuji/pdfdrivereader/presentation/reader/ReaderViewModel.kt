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
import kotlinx.coroutines.isActive
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
    private val saveCoverModeUseCase: SaveCoverModeUseCase,
    private val getPageImageUseCase: GetPageImageUseCase,
    private val getPageSizeUseCase: GetPageSizeUseCase,
    private val closeDocumentUseCase: CloseDocumentUseCase,
    private val appConfigRepository: AppConfigurationRepository,
    private val getTextSelectionUseCase: GetTextSelectionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderState())
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    private var cacheJob: Job? = null
    private var lastRefreshZoom: Float = 1.0f
    
    private var screenWidth: Int = 1080
    private var screenHeight: Int = 1920
    private var selectionJob: Job? = null

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
                val isCoverModeEnabled = openedDoc.isCoverModeEnabled
                val totalPages = openedDoc.document.totalPageCount
                val coverPages = openedDoc.document.coverPages
                
                val visiblePages = if (isCoverModeEnabled) {
                    (0 until totalPages).toList()
                } else {
                    (0 until totalPages).filter { it !in coverPages }
                }
                
                var startIndex = openedDoc.position.pageIndex
                if (!isCoverModeEnabled && startIndex in coverPages) {
                    startIndex = visiblePages.firstOrNull { it > startIndex } ?: visiblePages.lastOrNull() ?: 0
                }

                _state.update { 
                    it.copy(
                        document = openedDoc.document,
                        currentPage = startIndex,
                        zoomLevel = openedDoc.position.zoomLevel,
                        direction = openedDoc.direction,
                        isCoverModeEnabled = isCoverModeEnabled,
                        visiblePages = visiblePages
                    )
                }
                
                loadPageIntoCache(uri, startIndex)
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
        val visiblePages = _state.value.visiblePages
        val currentZoom = _state.value.zoomLevel
        val forceRefresh = currentZoom != lastRefreshZoom
        lastRefreshZoom = currentZoom
        
        if (visiblePages.isEmpty()) return

        val centerListIndex = visiblePages.indexOf(centerIndex).takeIf { it != -1 } ?: 0
        
        cacheJob?.cancel()
        cacheJob = viewModelScope.launch {
            
            // Priority 1: Current page
            if (forceRefresh || !_state.value.pageCache.containsKey(centerIndex)) {
                loadPageIntoCache(uri, centerIndex)
            }

            // Priority 2: Neighbor pages for concatenation
            val indicesToLoad = listOf(
                centerListIndex - 2, 
                centerListIndex - 1, 
                centerListIndex + 1, 
                centerListIndex + 2
            ).filter { it in visiblePages.indices }.map { visiblePages[it] }

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

    fun onCoverModeChanged(enabled: Boolean) {
        val document = _state.value.document ?: return
        val documentId = document.id
        if (enabled == _state.value.isCoverModeEnabled) return
        
        val newVisiblePages = if (enabled) {
            (0 until document.totalPageCount).toList()
        } else {
            (0 until document.totalPageCount).filter { it !in document.coverPages }
        }
        
        var newCurrentPage = _state.value.currentPage
        if (!enabled && newCurrentPage in document.coverPages) {
            newCurrentPage = newVisiblePages.firstOrNull { it > newCurrentPage } ?: newVisiblePages.lastOrNull() ?: 0
        }
        
        _state.update { 
            it.copy(
                isCoverModeEnabled = enabled, 
                visiblePages = newVisiblePages,
                currentPage = newCurrentPage
            ) 
        }
        performRefreshCache()
        
        viewModelScope.launch {
            saveCoverModeUseCase(documentId, enabled)
            if (newCurrentPage != _state.value.currentPage) {
                saveReadingPositionUseCase(documentId, PagePosition(newCurrentPage, _state.value.zoomLevel))
            }
        }
    }

    fun clearSelection() {
        _state.update { it.copy(textSelection = null) }
    }

    fun selectTextAt(pageIndex: Int, pdfX: Int, pdfY: Int) {
        val uri = _state.value.document?.id ?: return
        viewModelScope.launch {
            try {
                // To select a block of text near the point, we can pass a small bounding box
                // or just the exact point if the API handles it well. We pass the exact point.
                val selection = getTextSelectionUseCase(uri, pageIndex, pdfX, pdfY, pdfX, pdfY)
                _state.update { it.copy(textSelection = selection) }
            } catch (e: Exception) {
                Log.e("PDFDriveReader", "Failed to select text", e)
            }
        }
    }

    fun updateSelectionStart(pageIndex: Int, newStartX: Int, newStartY: Int) {
        val uri = _state.value.document?.id ?: return
        val stopHandle = _state.value.textSelection?.stopHandle ?: return
        selectionJob?.cancel()
        selectionJob = viewModelScope.launch {
            delay(30) // Debounce rapid drag events
            try {
                val selection = getTextSelectionUseCase(uri, pageIndex, newStartX, newStartY, stopHandle.x.toInt(), stopHandle.y.toInt())
                if (selection != null && isActive) {
                    _state.update { it.copy(textSelection = selection) }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.e("PDFDriveReader", "Failed to update start selection", e)
                }
            }
        }
    }

    fun updateSelectionStop(pageIndex: Int, newStopX: Int, newStopY: Int) {
        val uri = _state.value.document?.id ?: return
        val startHandle = _state.value.textSelection?.startHandle ?: return
        selectionJob?.cancel()
        selectionJob = viewModelScope.launch {
            delay(30) // Debounce rapid drag events
            try {
                val selection = getTextSelectionUseCase(uri, pageIndex, startHandle.x.toInt(), startHandle.y.toInt(), newStopX, newStopY)
                if (selection != null && isActive) {
                    _state.update { it.copy(textSelection = selection) }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.e("PDFDriveReader", "Failed to update stop selection", e)
                }
            }
        }
    }

    fun onDocumentTapped(pdfX: Int, pdfY: Int) {
        val selection = _state.value.textSelection
        if (selection != null) {
            val isInside = selection.bounds.any { rect ->
                pdfX >= rect.left && pdfX <= rect.right && pdfY >= rect.top && pdfY <= rect.bottom
            }
            if (!isInside) {
                clearSelection()
            }
        } else {
            toggleUI()
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
