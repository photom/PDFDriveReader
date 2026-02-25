package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.model.DomainResult
import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import javax.inject.Inject

/**
 * Use case for triggering a synchronization with Google Drive.
 */
class SyncCloudLibraryUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    /**
     * Executes the cloud synchronization and returns a result.
     */
    suspend operator fun invoke(): DomainResult<Unit> {
        return try {
            repository.syncCloud()
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error("Cloud synchronization failed: ${e.message}", e)
        }
    }
}
