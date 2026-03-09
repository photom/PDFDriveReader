package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import javax.inject.Inject

class SaveCoverModeUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    suspend operator fun invoke(uri: String, isCoverModeEnabled: Boolean) {
        repository.saveCoverMode(uri, isCoverModeEnabled)
    }
}
