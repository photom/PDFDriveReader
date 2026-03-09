package com.hitsuji.pdfdrivereader.domain.model

/**
 * The aggregate root for an active PDF document session.
 * 
 * @property id The unique identifier (URI).
 * @property fileName The name of the file to display in the UI.
 * @property totalPageCount The total number of pages in the document.
 * @property pageSizes The dimensions of each page in the document.
 * @property coverPages A set of page indices that are detected as cover pages.
 */
data class PdfDocument(
    val id: String,
    val fileName: String,
    val totalPageCount: Int,
    val pageSizes: List<PageDimension> = emptyList()
) {
    val coverPages: Set<Int>

    init {
        require(totalPageCount > 0) { "totalPageCount must be positive" }
        if (pageSizes.isNotEmpty()) {
            require(pageSizes.size == totalPageCount) { "pageSizes size must match totalPageCount" }
            
            // Detect cover pages
            val coverSet = mutableSetOf<Int>()
            if (pageSizes.size > 2) {
                // Find majority size by grouping and finding the max count
                val sizeCounts = pageSizes.groupingBy { it }.eachCount()
                val majoritySize = sizeCounts.maxByOrNull { it.value }?.key
                
                if (majoritySize != null) {
                    if (pageSizes.first() != majoritySize) {
                        coverSet.add(0)
                    }
                    if (pageSizes.last() != majoritySize) {
                        coverSet.add(pageSizes.lastIndex)
                    }
                }
            }
            coverPages = coverSet
        } else {
            coverPages = emptySet()
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
