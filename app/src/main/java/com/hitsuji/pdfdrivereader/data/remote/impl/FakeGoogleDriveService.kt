package com.hitsuji.pdfdrivereader.data.remote.impl

import com.hitsuji.pdfdrivereader.data.remote.GoogleDriveService
import com.hitsuji.pdfdrivereader.domain.model.DocumentMetadata
import com.hitsuji.pdfdrivereader.domain.model.SourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake implementation of [GoogleDriveService] for early development and testing.
 * 
 * Allows simulating cloud files, authentication states, and errors.
 */
class FakeGoogleDriveService : GoogleDriveService {

    private val _authState = MutableStateFlow(false)
    override val authState: StateFlow<Boolean> = _authState.asStateFlow()

    private val cloudFiles = mutableListOf<DocumentMetadata>()
    var shouldFail = false

    override fun getSignInIntent(): Any {
        return Any()
    }

    override fun handleSignInResult(account: Any) {
        _authState.value = true
    }

    override suspend fun signOut() {
        _authState.value = false
    }

    override suspend fun listFiles(): List<DocumentMetadata> {
        if (shouldFail) throw RuntimeException("Simulated Network Error")
        return if (_authState.value) cloudFiles else emptyList()
    }

    override suspend fun downloadFile(fileId: String, destination: String) {
        if (shouldFail) throw RuntimeException("Simulated Download Error")
    }

    /**
     * Helper for tests to pre-populate cloud files.
     */
    fun addCloudFile(id: String, name: String) {
        cloudFiles.add(
            DocumentMetadata(id, name, "Google Drive", SourceType.GOOGLE_DRIVE)
        )
    }
}
