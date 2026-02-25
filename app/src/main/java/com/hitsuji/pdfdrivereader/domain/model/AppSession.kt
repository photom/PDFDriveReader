package com.hitsuji.pdfdrivereader.domain.model

/**
 * Represents the last saved state of the entire application.
 * 
 * @property lastMode The last active [AppMode].
 * @property lastUri The URI of the last opened document, if any.
 */
data class AppSession(
    val lastMode: AppMode,
    val lastUri: String? = null
)

/**
 * High-level modes of the application.
 */
enum class AppMode {
    LIBRARY, READER
}
