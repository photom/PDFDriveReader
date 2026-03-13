package com.hitsuji.pdfdrivereader.domain.usecase

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.hitsuji.pdfdrivereader.domain.model.DocumentMetadata
import com.hitsuji.pdfdrivereader.domain.model.SourceType
import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Use case for manually adding local PDF files (from SAF picker) to the library.
 */
class AddLocalPdfsUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: PdfRepository
) {
    suspend operator fun invoke(uris: List<String>) {
        val metadataList = uris.map { uriString ->
            val uri = Uri.parse(uriString)
            val fileName = getFileName(uri) ?: "Unknown.pdf"
            DocumentMetadata(
                id = uriString,
                fileName = fileName,
                locationPath = "Picked Files",
                source = SourceType.LOCAL_STORAGE
            )
        }
        
        repository.saveMetadata(metadataList)
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}
