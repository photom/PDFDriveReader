package com.hitsuji.pdfdrivereader.domain.model

/**
 * The aggregate root for an active PDF document session.
 * 
 * @property id The unique identifier (URI).
 * @property fileName The name of the file to display in the UI.
 * @property totalPageCount The total number of pages in the document.
 * @property pageSizes The dimensions of each page in the document.
 */
data class PdfDocument(
    val id: String,
    val fileName: String,
    val totalPageCount: Int,
    val pageSizes: List<PageDimension> = emptyList()
) {
    init {
        require(totalPageCount > 0) { "totalPageCount must be positive" }
        if (pageSizes.isNotEmpty()) {
            require(pageSizes.size == totalPageCount) { "pageSizes size must match totalPageCount" }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PdfDocument) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
