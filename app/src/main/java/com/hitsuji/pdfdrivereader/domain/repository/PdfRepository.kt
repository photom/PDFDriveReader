package com.hitsuji.pdfdrivereader.domain.repository

import com.hitsuji.pdfdrivereader.domain.model.DocumentMetadata
import com.hitsuji.pdfdrivereader.domain.model.PagePosition
import com.hitsuji.pdfdrivereader.domain.model.PdfDocument
import com.hitsuji.pdfdrivereader.domain.model.ReadingDirection
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing PDF document metadata, persistence, and synchronization.
 * 
 * This interface defines the contract for accessing and modifying document data
 * from local and cloud sources.
 */
interface PdfRepository {
    /**
     * Observes a reactive list of all documents (Local + Cloud).
     * 
     * @return A [Flow] emitting the current list of [DocumentMetadata].
     */
    fun getDocuments(): Flow<List<DocumentMetadata>>

    /**
     * Fetches a specific document by its unique identifier.
     * 
     * @param uri The persistent URI or Drive ID.
     * @return The [PdfDocument] aggregate root.
     */
    suspend fun getDocument(uri: String): PdfDocument

    /**
     * Persists the current reading position for a specific document.
     * 
     * @param uri The document identifier.
     * @param position The [PagePosition] to save.
     */
    suspend fun savePosition(uri: String, position: PagePosition)

    /**
     * Retrieves the last saved position for a specific document.
     * 
     * @param uri The document identifier.
     * @return The saved [PagePosition], or null if never opened.
     */
    suspend fun getSavedPosition(uri: String): PagePosition?

    /**
     * Persists the reading direction preference for a specific document.
     * 
     * @param uri The document identifier.
     * @param direction The [ReadingDirection] to save.
     */
    suspend fun saveDirection(uri: String, direction: ReadingDirection)

    /**
     * Retrieves the saved reading direction for a specific document.
     * 
     * @param uri The document identifier.
     * @return The saved [ReadingDirection], or null if never configured.
     */
    suspend fun getSavedDirection(uri: String): ReadingDirection?

    /**
     * Retrieves the original size of a specific page.
     * 
     * @return Pair of (Width, Height).
     */
    suspend fun getPageSize(uri: String, pageIndex: Int): Pair<Int, Int>

    /**
     * Retrieves a rendered image of a specific page.
     * 
     * @param uri The document identifier.
     * @param pageIndex The 0-based page index.
     * @param width The target width in pixels.
     * @param height The target height in pixels.
     * @return The rendered page image (Implementation specific, e.g., Bitmap).
     */
    suspend fun getPageImage(uri: String, pageIndex: Int, width: Int, height: Int): Any

    /**
     * Closes the active document and releases system resources.
     */
    suspend fun closeDocument()

    /**
     * Triggers an asynchronous scan of the local filesystem for new PDFs.
     */
    suspend fun syncLocal()

    /**
     * Triggers an asynchronous synchronization with Google Drive.
     */
    suspend fun syncCloud()
}
