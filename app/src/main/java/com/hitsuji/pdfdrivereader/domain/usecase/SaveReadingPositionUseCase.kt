package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.model.PagePosition
import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import javax.inject.Inject

/**
 * Use case for persisting the user's reading position within a specific document.
 * 
 * @property repository The [PdfRepository] to interact with.
 */
class SaveReadingPositionUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    /**
     * Executes the save position logic.
     * 
     * @param uri The document identifier.
     * @param position The current [PagePosition] to persist.
     */
    suspend operator fun invoke(uri: String, position: PagePosition) {
        repository.savePosition(uri, position)
    }
}
