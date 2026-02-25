package com.hitsuji.pdfdrivereader.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.hitsuji.pdfdrivereader.data.local.entity.DocumentMetadataEntity
import com.hitsuji.pdfdrivereader.data.local.entity.NavHistoryEntity
import com.hitsuji.pdfdrivereader.data.local.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for PDF-related persistence operations.
 */
@Dao
interface PdfDao {

    /**
     * Inserts or updates document metadata.
     * 
     * @param entity The document metadata to store.
     */
    @Upsert
    suspend fun upsertMetadata(entity: DocumentMetadataEntity)

    /**
     * Retrieves all cached document metadata, sorted by last modified date.
     * 
     * @return A [Flow] of document metadata lists.
     */
    @Query("SELECT * FROM DOCUMENT_METADATA ORDER BY last_modified DESC")
    fun getAllMetadata(): Flow<List<DocumentMetadataEntity>>

    /**
     * Retrieves metadata for a specific document.
     * 
     * @param uri The unique identifier of the file.
     * @return The metadata entity or null.
     */
    @Query("SELECT * FROM DOCUMENT_METADATA WHERE file_uri = :uri")
    suspend fun getMetadataByUri(uri: String): DocumentMetadataEntity?

    /**
     * Inserts or updates a reading session.
     * 
     * @param entity The session state to persist.
     */
    @Upsert
    suspend fun upsertSession(entity: ReadingSessionEntity)

    /**
     * Retrieves the reading session for a specific document.
     * 
     * @param uri The unique identifier of the file.
     * @return The session entity or null.
     */
    @Query("SELECT * FROM READING_SESSION WHERE file_uri = :uri")
    suspend fun getSessionByUri(uri: String): ReadingSessionEntity?

    /**
     * Adds an entry to the navigation history stack.
     * 
     * @param entity The history entry to add.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(entity: NavHistoryEntity)

    /**
     * Retrieves the navigation history stack for a document, ordered by timestamp (newest first).
     * 
     * @param uri The unique identifier of the file.
     * @return A list of history entries.
     */
    @Query("SELECT * FROM NAV_HISTORY WHERE file_uri = :uri ORDER BY timestamp DESC")
    suspend fun getHistoryByUri(uri: String): List<NavHistoryEntity>

    /**
     * Deletes the most recent history entry for a document.
     * 
     * @param uri The unique identifier of the file.
     */
    @Query("DELETE FROM NAV_HISTORY WHERE id = (SELECT id FROM NAV_HISTORY WHERE file_uri = :uri ORDER BY timestamp DESC LIMIT 1)")
    suspend fun popHistoryEntry(uri: String)
}
