package com.hitsuji.pdfdrivereader.domain.model

/**
 * Represents a specific location within a PDF document.
 * 
 * @property pageIndex The 0-based index of the page.
 * @property zoomLevel The zoom scale factor (1.0 to 5.0).
 */
data class PagePosition(
    val pageIndex: Int,
    val zoomLevel: Float
) {
    init {
        require(pageIndex >= 0) { "pageIndex must be non-negative" }
        require(zoomLevel in 1.0f..5.0f) { "zoomLevel must be between 1.0 and 5.0" }
    }
}
