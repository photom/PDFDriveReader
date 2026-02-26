package com.hitsuji.pdfdrivereader.data.repository

import com.hitsuji.pdfdrivereader.data.local.dao.PdfDao
import com.hitsuji.pdfdrivereader.data.local.scanner.LocalFileScanner
import com.hitsuji.pdfdrivereader.data.mapper.DocumentMapper
import com.hitsuji.pdfdrivereader.data.mapper.ReadingSessionMapper
import com.hitsuji.pdfdrivereader.data.remote.GoogleDriveService
import com.hitsuji.pdfdrivereader.data.renderer.PdfRendererWrapper
import com.hitsuji.pdfdrivereader.domain.model.DocumentMetadata
import com.hitsuji.pdfdrivereader.domain.model.PagePosition
import com.hitsuji.pdfdrivereader.domain.model.PdfDocument
import com.hitsuji.pdfdrivereader.domain.model.ReadingDirection
import com.hitsuji.pdfdrivereader.domain.model.ReadingSettings
import com.hitsuji.pdfdrivereader.domain.repository.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Room-backed implementation of the [PdfRepository].
 */
class RoomPdfRepository @Inject constructor(
    private val dao: PdfDao,
    private val documentMapper: DocumentMapper,
    private val sessionMapper: ReadingSessionMapper,
    private val scanner: LocalFileScanner,
    private val renderer: PdfRendererWrapper,
    private val driveService: GoogleDriveService
) : PdfRepository {

    override fun getDocuments(): Flow<List<DocumentMetadata>> {
        return dao.getAllMetadata().map { entities ->
            entities.map { documentMapper.toDomain(it) }
        }
    }

    private suspend fun resolveUriToFile(uri: String): File {
        val metadata = dao.getMetadataByUri(uri)
            ?: throw IllegalStateException("Metadata not found for: $uri")

        return if (metadata.sourceType == "GOOGLE_DRIVE") {
            val cacheFile = File(scanner.getCloudCacheDir(), uri)
            if (!cacheFile.exists()) {
                driveService.downloadFile(uri, cacheFile.absolutePath)
            }
            cacheFile
        } else {
            File(uri)
        }
    }

    override suspend fun getDocument(uri: String): PdfDocument = withContext(Dispatchers.IO) {
        val file = resolveUriToFile(uri)
        if (!file.exists()) {
            throw IllegalStateException("PDF file not found at: ${file.absolutePath}")
        }
        renderer.openDocument(file).copy(id = uri)
    }

    override suspend fun savePosition(uri: String, position: PagePosition) = withContext(Dispatchers.IO) {
        val existingSession = dao.getSessionByUri(uri)
        val direction = existingSession?.let { 
            ReadingDirection.valueOf(it.readingDirection) 
        } ?: ReadingDirection.LTR
        
        val entity = sessionMapper.toEntity(uri, position, ReadingSettings(direction))
        dao.upsertSession(entity)
    }

    override suspend fun getSavedPosition(uri: String): PagePosition? = withContext(Dispatchers.IO) {
        dao.getSessionByUri(uri)?.let {
            sessionMapper.toDomain(it).first
        }
    }

    override suspend fun saveDirection(uri: String, direction: ReadingDirection) = withContext(Dispatchers.IO) {
        val position = getSavedPosition(uri) ?: PagePosition(0, 1.0f)
        val entity = sessionMapper.toEntity(uri, position, ReadingSettings(direction))
        dao.upsertSession(entity)
    }

    override suspend fun getSavedDirection(uri: String): ReadingDirection? = withContext(Dispatchers.IO) {
        dao.getSessionByUri(uri)?.let {
            sessionMapper.toDomain(it).second.direction
        }
    }

    override suspend fun getPageSize(uri: String, pageIndex: Int): Pair<Int, Int> = withContext(Dispatchers.IO) {
        // Stateful renderer is already open after getDocument call
        renderer.getPageSize(pageIndex)
    }

    override suspend fun getPageImage(
        uri: String, 
        pageIndex: Int, 
        width: Int, 
        height: Int
    ): Any = withContext(Dispatchers.IO) {
        // No file opening here! Uses the active session from Step 1.
        renderer.renderPage(pageIndex, width, height)
    }

    override suspend fun closeDocument() = withContext(Dispatchers.IO) {
        renderer.close()
    }

    override suspend fun syncLocal() = withContext(Dispatchers.IO) {
        val foundDocs = scanner.scanDevice()
        foundDocs.forEach { doc ->
            dao.upsertMetadata(documentMapper.toEntity(doc))
        }
    }

    override suspend fun syncCloud() = withContext(Dispatchers.IO) {
        val cloudDocs = driveService.listFiles()
        cloudDocs.forEach { doc ->
            dao.upsertMetadata(documentMapper.toEntity(doc))
        }
    }
}
