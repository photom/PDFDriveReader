package com.hitsuji.pdfdrivereader.data.remote

import com.hitsuji.pdfdrivereader.domain.model.DocumentMetadata
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for interacting with the Google Drive API.
 */
interface GoogleDriveService {
    /**
     * Emits the current authentication state.
     */
    val authState: StateFlow<Boolean>

    /**
     * Returns the intent required to start the Google Sign-In flow.
     */
    fun getSignInIntent(): Any

    /**
     * Updates the service state with a successful sign-in result.
     * 
     * @param account The Google account object (implementation specific).
     */
    fun handleSignInResult(account: Any)

    /**
     * Signs out the current user.
     */
    suspend fun signOut()

    /**
     * Lists all PDF files in the user's Google Drive.
     * 
     * @return A list of [DocumentMetadata] for each cloud PDF.
     */
    suspend fun listFiles(): List<DocumentMetadata>

    /**
     * Downloads a specific file from Google Drive to a local temporary file.
     * 
     * @param fileId The unique Drive ID.
     * @param destination Absolute path where the file should be saved.
     */
    suspend fun downloadFile(fileId: String, destination: String)
}
