package com.hitsuji.pdfdrivereader.data.repository

import com.hitsuji.pdfdrivereader.data.local.dao.PdfDao
import com.hitsuji.pdfdrivereader.data.local.entity.ReadingSessionEntity
import com.hitsuji.pdfdrivereader.data.local.scanner.LocalFileScanner
import com.hitsuji.pdfdrivereader.data.mapper.DocumentMapper
import com.hitsuji.pdfdrivereader.data.mapper.ReadingSessionMapper
import com.hitsuji.pdfdrivereader.data.remote.impl.FakeGoogleDriveService
import com.hitsuji.pdfdrivereader.data.renderer.PdfRendererWrapper
import com.hitsuji.pdfdrivereader.domain.model.PagePosition
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for the [RoomPdfRepository].
 */
class RoomPdfRepositoryTest {

    private val dao: PdfDao = mock()
    private val scanner: LocalFileScanner = mock()
    private val renderer: PdfRendererWrapper = mock()
    private val driveService = FakeGoogleDriveService()
    private val documentMapper = DocumentMapper()
    private val sessionMapper = ReadingSessionMapper()
    
    private val repository = RoomPdfRepository(
        dao, documentMapper, sessionMapper, scanner, renderer, driveService
    )

    /**
     * Verifies that [RoomPdfRepository] correctly retrieves and maps saved positions.
     */
    @Test
    fun `getSavedPosition should return mapped position from DAO`() = runTest {
        val uri = "uri1"
        val sessionEntity = ReadingSessionEntity(uri, 5, "LTR", 1.0f)
        val expectedPosition = PagePosition(5, 1.0f)

        whenever(dao.getSessionByUri(uri)) doReturn sessionEntity

        val result = repository.getSavedPosition(uri)

        assertEquals(expectedPosition, result)
        verify(dao).getSessionByUri(uri)
    }

    /**
     * Verifies that [RoomPdfRepository] correctly handles and maps position saving.
     */
    @Test
    fun `savePosition should upsert session through DAO`() = runTest {
        val uri = "uri1"
        val position = PagePosition(10, 2.0f)
        
        whenever(dao.getSessionByUri(uri)) doReturn null 
        
        repository.savePosition(uri, position)
        
        verify(dao).upsertSession(check {
            assertEquals(uri, it.fileUri)
            assertEquals(10, it.currentPage)
            assertEquals(2.0f, it.zoomLevel)
        })
    }

    /**
     * Verifies that syncCloud correctly fetches files and updates the DAO with resolved folder names.
     */
    @Test
    fun `syncCloud should fetch files from drive and update DAO with folder names`() = runTest {
        driveService.handleSignInResult(Any())
        driveService.addFolder("folderId1", "My Books")
        driveService.addCloudFile("drive1", "Cloud.pdf", "folderId1")
        
        repository.syncCloud()
        
        verify(dao).upsertMetadata(check {
            assertEquals("drive1", it.fileUri)
            assertEquals("Cloud.pdf", it.fileName)
            assertEquals("My Books", it.locationPath)
        })
    }
}
