package com.hitsuji.pdfdrivereader.domain.usecase

import com.hitsuji.pdfdrivereader.domain.model.DocumentMetadata
import com.hitsuji.pdfdrivereader.domain.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SearchLibraryUseCaseTest {

    private lateinit var searchLibraryUseCase: SearchLibraryUseCase
    private lateinit var documents: List<DocumentMetadata>

    @Before
    fun setup() {
        searchLibraryUseCase = SearchLibraryUseCase()
        documents = listOf(
            DocumentMetadata("1", "Kotlin Guide.pdf", "/Books/Programming", SourceType.LOCAL_STORAGE),
            DocumentMetadata("2", "Android Dev.pdf", "/Downloads", SourceType.LOCAL_STORAGE),
            DocumentMetadata("3", "Receipt.pdf", "/Documents/Finance", SourceType.LOCAL_STORAGE),
            DocumentMetadata("4", "KOTLIN in Action.pdf", "Google Drive", SourceType.GOOGLE_DRIVE)
        )
    }

    @Test
    fun `Filtering partial match on filename`() {
        val result = searchLibraryUseCase(documents, "android")
        assertEquals(1, result.size)
        assertEquals("Android Dev.pdf", result[0].fileName)
    }

    @Test
    fun `Filtering partial match on location path`() {
        val result = searchLibraryUseCase(documents, "finance")
        assertEquals(1, result.size)
        assertEquals("Receipt.pdf", result[0].fileName)
    }

    @Test
    fun `Formatting ignores case during matching`() {
        val result = searchLibraryUseCase(documents, "kotlin")
        assertEquals(2, result.size)
        val names = result.map { it.fileName }
        assert(names.contains("Kotlin Guide.pdf"))
        assert(names.contains("KOTLIN in Action.pdf"))
    }

    @Test
    fun `Edge Case returns all files for empty or whitespace-only queries`() {
        var result = searchLibraryUseCase(documents, "")
        assertEquals(4, result.size)
        
        result = searchLibraryUseCase(documents, "   ")
        assertEquals(4, result.size)
    }

    @Test
    fun `Edge Case correctly handles special regex characters without crashing`() {
        val result = searchLibraryUseCase(documents, ".*[?")
        assertEquals(0, result.size)
    }
}
