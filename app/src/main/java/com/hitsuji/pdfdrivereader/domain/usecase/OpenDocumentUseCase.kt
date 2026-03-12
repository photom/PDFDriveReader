package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.model.PagePosition
import com.hitsuji.pdfdrivereader.domain.model.PdfDocument
import com.hitsuji.pdfdrivereader.domain.model.ReadingDirection
import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import javax.inject.Inject

/**
 * Result of the [OpenDocumentUseCase] containing the document and its restored state.
 */
data class OpenedDocument(
    val document: PdfDocument,
    val position: PagePosition,
    val direction: ReadingDirection
)

/**
 * Use case for opening a PDF document and restoring its last saved reading state.
 * 
 * @property repository The [PdfRepository] to interact with.
 */
class OpenDocumentUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    /**
     * Executes the open document logic.
     * 
     * @param uri The document identifier.
     * @return An [OpenedDocument] containing the metadata and settings.
     */
    suspend operator fun invoke(uri: String): OpenedDocument {
        val document = repository.getDocument(uri)
        val position = repository.getSavedPosition(uri) ?: PagePosition(0, 1.0f)
        val direction = repository.getSavedDirection(uri) ?: ReadingDirection.LTR
        
        return OpenedDocument(document, position, direction)
    }
}
