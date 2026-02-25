package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import javax.inject.Inject

/**
 * Use case for retrieving a rendered image of a specific PDF page.
 * 
 * @property repository The [PdfRepository] to interact with.
 */
class GetPageImageUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    /**
     * Executes the page rendering logic.
     * 
     * @param uri The document identifier.
     * @param pageIndex The 0-based page index.
     * @param width The target width in pixels.
     * @param height The target height in pixels.
     * @return The rendered page image.
     */
    suspend operator fun invoke(uri: String, pageIndex: Int, width: Int, height: Int): Any {
        return repository.getPageImage(uri, pageIndex, width, height)
    }
}
