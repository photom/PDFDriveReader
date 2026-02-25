package com.hitsuji.pdfdrivereader.presentation.reader

import android.graphics.Bitmap
import com.hitsuji.pdfdrivereader.domain.model.PdfDocument
import com.hitsuji.pdfdrivereader.domain.model.ReadingDirection

/**
 * Represents the UI state for the Reader screen.
 */
data class ReaderState(
    val isLoading: Boolean = true,
    val document: PdfDocument? = null,
    val isUiVisible: Boolean = false,
    val currentPage: Int = 0,
    val pageCache: Map<Int, Bitmap> = emptyMap(),
    val zoomLevel: Float = 1.0f,
    val direction: ReadingDirection = ReadingDirection.LTR,
    val errorMessage: String? = null
)
