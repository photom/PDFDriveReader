package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.model.DocumentMetadata
import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import kotlinx.coroutines.flow.Flow

import javax.inject.Inject

/**
 * Use case for observing the collection of all PDF documents (Local + Cloud).
 * 
 * @property repository The [PdfRepository] to interact with.
 */
class GetDocumentsUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    /**
     * Executes the observation logic.
     * 
     * @return A [Flow] emitting updated lists of [DocumentMetadata].
     */
    operator fun invoke(): Flow<List<DocumentMetadata>> {
        return repository.getDocuments()
    }
}
