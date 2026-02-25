package com.hitsuji.pdfdrivereader.domain.model

/**
 * The origin of a PDF document.
 */
enum class SourceType {
    /** Files found on the device's local storage */
    LOCAL_STORAGE, 
    /** Files synced from Google Drive */
    GOOGLE_DRIVE
}
