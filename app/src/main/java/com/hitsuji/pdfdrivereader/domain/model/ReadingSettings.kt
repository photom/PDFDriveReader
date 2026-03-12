package com.hitsuji.pdfdrivereader.domain.model

/**
 * User-configurable settings applied specifically to a document.
 * 
 * @property direction The chosen [ReadingDirection] for the document.
 * @property savedZoom The preferred zoom scale factor (1.0 to 5.0).
 */
data class ReadingSettings(
    val direction: ReadingDirection = ReadingDirection.LTR,
    val savedZoom: Float = 1.0f
)
