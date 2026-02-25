package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.model.ReadingDirection
import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import javax.inject.Inject

/**
 * Use case for persisting the user's reading direction preference for a specific document.
 * 
 * @property repository The [PdfRepository] to interact with.
 */
class SaveReadingDirectionUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    /**
     * Executes the save direction logic.
     * 
     * @param uri The document identifier.
     * @param direction The preferred [ReadingDirection] to persist.
     */
    suspend operator fun invoke(uri: String, direction: ReadingDirection) {
        repository.saveDirection(uri, direction)
    }
}
