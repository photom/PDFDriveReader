package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.model.DocumentMetadata
import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
     * Executes the observation logic and sorts the results.
     * 
     * The list is sorted primary by [DocumentMetadata.locationPath] and 
     * secondary by [DocumentMetadata.fileName] in alphabetical order.
     * 
     * @return A [Flow] emitting updated and sorted lists of [DocumentMetadata].
     */
    operator fun invoke(): Flow<List<DocumentMetadata>> {
        return repository.getDocuments().map { docs ->
            docs.sortedWith(
                compareBy<DocumentMetadata> { it.locationPath }
                    .thenBy { it.fileName }
            )
        }
    }
}
