package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.model.DocumentMetadata
import javax.inject.Inject

/**
 * Use case for filtering a list of documents based on a search query.
 */
class SearchLibraryUseCase @Inject constructor() {

    /**
     * Filters the provided list of documents.
     * 
     * @param documents The list of documents to filter.
     * @param query The search string.
     * @return A filtered list where the file name or location path matches the query.
     */
    operator fun invoke(documents: List<DocumentMetadata>, query: String): List<DocumentMetadata> {
        if (query.isBlank()) return documents

        return documents.filter { doc ->
            doc.fileName.contains(query, ignoreCase = true) ||
            doc.locationPath.contains(query, ignoreCase = true)
        }
    }
}
