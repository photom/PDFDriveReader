package com.hitsuji.pdfdrivereader.presentation.library

import com.hitsuji.pdfdrivereader.domain.model.DocumentMetadata

/**
 * Represents the UI state for the Library screen.
 */
sealed class LibraryState {
    /**
     * Initial state while fetching documents or during synchronization.
     */
    object Loading : LibraryState()

    /**
     * State when documents are successfully loaded.
     * 
     * @property documents The list of [DocumentMetadata] to display.
     */
    data class Success(val documents: List<DocumentMetadata>) : LibraryState()

    /**
     * State when no documents are found in the library.
     */
    object Empty : LibraryState()

    /**
     * State when an error occurred during document retrieval.
     * 
     * @property message Descriptive error message.
     */
    data class Error(val message: String) : LibraryState()
}
