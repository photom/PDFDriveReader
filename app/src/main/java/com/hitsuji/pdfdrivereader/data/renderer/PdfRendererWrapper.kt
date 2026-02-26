package com.hitsuji.pdfdrivereader.data.renderer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import com.hitsuji.pdfdrivereader.domain.model.PdfDocument
import java.io.File

/**
 * Stateful wrapper for the native [PdfRenderer].
 * 
 * Maintains an open document session to optimize multi-page rendering.
 */
class PdfRendererWrapper {

    private var activePfd: ParcelFileDescriptor? = null
    private var activeRenderer: PdfRenderer? = null
    private var activeFile: File? = null

    /**
     * Opens a PDF file and caches the renderer for subsequent page requests.
     * 
     * @param file The PDF file.
     * @return [PdfDocument] metadata.
     */
    fun openDocument(file: File): PdfDocument {
        if (activeFile?.absolutePath == file.absolutePath && activeRenderer != null) {
            return PdfDocument(file.absolutePath, file.name, activeRenderer!!.pageCount)
        }

        close() // Close existing session if any

        Log.d("PDFDriveReader", "PdfRenderer: Opening NEW session for ${file.name}")
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            activePfd = pfd
            activeRenderer = renderer
            activeFile = file
            
            return PdfDocument(id = file.absolutePath, fileName = file.name, totalPageCount = renderer.pageCount)
        } catch (e: Exception) {
            Log.e("PDFDriveReader", "PdfRenderer: Failed to open document", e)
            throw e
        }
    }

    /**
     * Retrieves the original size of a specific page from the active session.
     */
    fun getPageSize(pageIndex: Int): Pair<Int, Int> {
        val renderer = activeRenderer ?: throw IllegalStateException("No active PDF session")
        var page: PdfRenderer.Page? = null
        try {
            page = renderer.openPage(pageIndex)
            return Pair(page.width, page.height)
        } finally {
            page?.close()
        }
    }

    /**
     * Renders a page using the currently active renderer.
     */
    fun renderPage(pageIndex: Int, width: Int, height: Int): Bitmap {
        val renderer = activeRenderer ?: throw IllegalStateException("No active PDF session")
        
        var page: PdfRenderer.Page? = null
        try {
            page = renderer.openPage(pageIndex)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        } catch (e: Exception) {
            Log.e("PDFDriveReader", "PdfRenderer: Failed to render page $pageIndex", e)
            throw e
        } finally {
            page?.close()
        }
    }

    /**
     * Closes the active PDF session and releases all native resources.
     */
    fun close() {
        activeRenderer?.close()
        activePfd?.close()
        activeRenderer = null
        activePfd = null
        activeFile = null
        Log.d("PDFDriveReader", "PdfRenderer: Session closed")
    }
}
