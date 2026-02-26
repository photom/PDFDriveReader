package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import javax.inject.Inject

/**
 * Use case for closing the active PDF document and releasing resources.
 */
class CloseDocumentUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    suspend operator fun invoke() {
        repository.closeDocument()
    }
}
