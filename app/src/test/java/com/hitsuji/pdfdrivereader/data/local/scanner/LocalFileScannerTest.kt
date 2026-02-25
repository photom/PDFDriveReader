package com.hitsuji.pdfdrivereader.data.local.scanner

import android.content.Context
import com.hitsuji.pdfdrivereader.domain.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

/**
 * Unit tests for the [LocalFileScanner].
 */
class LocalFileScannerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()
    
    private val context: Context = mock()
    private lateinit var scanner: LocalFileScanner

    @Before
    fun setup() {
        val cacheDir = tempFolder.newFolder("cache")
        whenever(context.cacheDir) doReturn cacheDir
        scanner = LocalFileScanner(context)
    }

    @Test
    fun `scan should return list of PDF files in directory`() {
        val root = tempFolder.newFolder("pdfs")
        File(root, "test1.pdf").createNewFile()
        File(root, "test2.pdf").createNewFile()
        File(root, "ignore.txt").createNewFile()
        
        val result = scanner.scan(root)
        
        assertEquals(2, result.size)
        val fileNames = result.map { it.fileName }
        assertTrue(fileNames.contains("test1.pdf"))
        assertTrue(fileNames.contains("test2.pdf"))
        result.forEach { 
            assertEquals(SourceType.LOCAL_STORAGE, it.source)
        }
    }

    @Test
    fun `getCloudCacheDir should create and return directory`() {
        val dir = scanner.getCloudCacheDir()
        assertTrue(dir.exists())
        assertTrue(dir.isDirectory)
        assertEquals("cloud_docs", dir.name)
    }
}
