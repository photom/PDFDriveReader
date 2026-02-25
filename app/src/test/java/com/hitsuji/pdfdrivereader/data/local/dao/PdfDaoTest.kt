package com.hitsuji.pdfdrivereader.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hitsuji.pdfdrivereader.data.local.AppDatabase
import com.hitsuji.pdfdrivereader.data.local.entity.DocumentMetadataEntity
import com.hitsuji.pdfdrivereader.data.local.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Unit tests for the [PdfDao] using Robolectric.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34]) // Robolectric supports up to 34
class PdfDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: PdfDao

    @Before
    fun `setup database`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.pdfDao()
    }

    @After
    fun `close database`() {
        database.close()
    }

    @Test
    fun `upsertMetadata should store and retrieve document metadata`() = runTest {
        val entity = DocumentMetadataEntity("uri1", "name.pdf", "/path/", "LOCAL_STORAGE", 123L)
        dao.upsertMetadata(entity)
        
        val result = dao.getAllMetadata().first()
        assertEquals(1, result.size)
        assertEquals(entity, result[0])
    }

    @Test
    fun `upsertSession should store and update reading session state`() = runTest {
        val metadata = DocumentMetadataEntity("uri1", "name.pdf", "/path/", "LOCAL_STORAGE", 123L)
        dao.upsertMetadata(metadata)
        
        val session = ReadingSessionEntity("uri1", 5, "LTR", 1.0f)
        dao.upsertSession(session)
        
        val result = dao.getSessionByUri("uri1")
        assertNotNull(result)
        assertEquals(5, result?.currentPage)
        
        // Update
        val updatedSession = session.copy(currentPage = 10)
        dao.upsertSession(updatedSession)
        
        val updatedResult = dao.getSessionByUri("uri1")
        assertEquals(10, updatedResult?.currentPage)
    }
}
