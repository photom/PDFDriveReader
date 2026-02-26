package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import javax.inject.Inject

/**
 * Use case for retrieving the original dimensions of a specific PDF page.
 * 
 * @property repository The [PdfRepository] to interact with.
 */
class GetPageSizeUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    /**
     * Executes the page size retrieval logic.
     * 
     * @param uri The document identifier.
     * @param pageIndex The 0-based page index.
     * @return A Pair containing (Width, Height).
     */
    suspend operator fun invoke(uri: String, pageIndex: Int): Pair<Int, Int> {
        return repository.getPageSize(uri, pageIndex)
    }
}
