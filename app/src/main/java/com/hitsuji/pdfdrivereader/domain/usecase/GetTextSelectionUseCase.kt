package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.model.PdfTextSelection
import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import javax.inject.Inject

class GetTextSelectionUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    suspend operator fun invoke(uri: String, pageIndex: Int, startX: Int, startY: Int, stopX: Int, stopY: Int): PdfTextSelection? {
        return repository.selectText(uri, pageIndex, startX, startY, stopX, stopY)
    }
}