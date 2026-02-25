package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.model.DomainResult
import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import javax.inject.Inject

/**
 * Use case for triggering a scan of the local filesystem for PDF documents.
 */
class SyncLocalLibraryUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    /**
     * Executes the local synchronization and returns a result.
     */
    suspend operator fun invoke(): DomainResult<Unit> {
        return try {
            repository.syncLocal()
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error("Local scan failed: ${e.message}", e)
        }
    }
}
