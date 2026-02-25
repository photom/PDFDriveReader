package com.hitsuji.pdfdrivereader.domain.model

/**
 * Basic information about a PDF file used for library listing.
 * 
 * @property id The unique identifier (usually the URI or Google Drive ID).
 * @property fileName The display name of the file.
 * @property locationPath The human-readable location (folder path or breadcrumb).
 * @property source The [SourceType] identifying where the file is stored.
 * @property isCached True if the document is available in local storage (always true for local source).
 */
data class DocumentMetadata(
    val id: String,
    val fileName: String,
    val locationPath: String,
    val source: SourceType,
    val isCached: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DocumentMetadata) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
