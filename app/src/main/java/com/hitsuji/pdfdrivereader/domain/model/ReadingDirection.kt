package com.hitsuji.pdfdrivereader.domain.model

/**
 * Defines the flow and snapping behavior of the PDF reader.
 */
enum class ReadingDirection {
    /** Left-to-Right (standard novels/textbooks) */
    LTR, 
    /** Right-to-Left (Manga, Arabic, Hebrew) */
    RTL, 
    /** Top-to-Bottom (Technical documents, continuous scroll) */
    TTB
}
