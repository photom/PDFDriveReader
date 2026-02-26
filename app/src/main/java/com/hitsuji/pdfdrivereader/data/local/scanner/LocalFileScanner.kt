package com.hitsuji.pdfdrivereader.data.local.scanner

import android.content.ContentResolver
import android.provider.MediaStore
import com.hitsuji.pdfdrivereader.domain.model.DocumentMetadata
import com.hitsuji.pdfdrivereader.domain.model.SourceType
import java.io.File

/**
 * Service responsible for scanning the local filesystem for PDF documents using MediaStore.
 */
class LocalFileScanner(private val context: android.content.Context) {

    /**
     * Returns the internal directory used for caching cloud documents.
     */
    fun getCloudCacheDir(): File {
        val dir = File(context.cacheDir, "cloud_docs")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Scans the device for PDF documents using the MediaStore API.
     * 
     * @return A list of [DocumentMetadata] for each PDF found.
     */
    fun scanDevice(): List<DocumentMetadata> {
        val pdfList = mutableListOf<DocumentMetadata>()
        val contentResolver: ContentResolver? = try { context.contentResolver } catch (e: Exception) { null }
        
        if (contentResolver == null) return emptyList()

        val collections = mutableListOf<android.net.Uri>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            collections.add(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL))
            collections.add(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL))
        } else {
            collections.add(MediaStore.Files.getContentUri("external"))
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.RELATIVE_PATH
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("application/pdf", "%.pdf")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        val foundPaths = mutableSetOf<String>()

        for (collection in collections) {
            try {
                contentResolver.query(
                    collection,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    val nameColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val dataColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                    val pathColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH)

                    while (cursor.moveToNext()) {
                        val name = if (nameColumn != -1) cursor.getString(nameColumn) else "Unknown"
                        val fullPath = if (dataColumn != -1) cursor.getString(dataColumn) else ""
                        val relativePath = if (pathColumn != -1) cursor.getString(pathColumn) else "/"

                        if (fullPath.isNotEmpty() && fullPath.endsWith(".pdf", ignoreCase = true) && !foundPaths.contains(fullPath)) {
                            foundPaths.add(fullPath)
                            pdfList.add(
                                DocumentMetadata(
                                    id = fullPath,
                                    fileName = name ?: "Unknown",
                                    locationPath = relativePath ?: "/",
                                    source = SourceType.LOCAL_STORAGE
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently fail for this collection and continue
            }
        }
        
        return pdfList
    }

    /**
     * Scans a specific directory for files with the .pdf extension.
     * 
     * @param root The directory to scan.
     * @return A list of [DocumentMetadata] for each PDF found.
     */
    fun scan(root: File): List<DocumentMetadata> {
        return root.listFiles { file -> 
            file.isFile && file.extension.equals("pdf", ignoreCase = true) 
        }?.map { file ->
            DocumentMetadata(
                id = file.absolutePath,
                fileName = file.name,
                locationPath = file.parent ?: "/",
                source = SourceType.LOCAL_STORAGE
            )
        } ?: emptyList()
    }
}
