package com.hitsuji.pdfdrivereader.data.renderer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
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
    private var activeId: String? = null

    /**
     * Opens a PDF file from a URI and caches the renderer for subsequent page requests.
     */
    fun openDocument(context: Context, uri: Uri, fileName: String): PdfDocument {
        if (activeId == uri.toString() && activeRenderer != null) {
            val sizes = (0 until activeRenderer!!.pageCount).map {
                val size = getPageSize(it)
                com.hitsuji.pdfdrivereader.domain.model.PageDimension(size.first, size.second)
            }
            return PdfDocument(uri.toString(), fileName, activeRenderer!!.pageCount, sizes)
        }

        close() // Close existing session if any

        Log.d("PDFDriveReader", "PdfRenderer: Opening NEW session for $fileName")
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: throw IllegalStateException("Could not open file descriptor for $uri")
            
            val renderer = PdfRenderer(pfd)
            activePfd = pfd
            activeRenderer = renderer
            activeId = uri.toString()
            
            val pageCount = renderer.pageCount
            val sizes = (0 until pageCount).map { index ->
                var page: PdfRenderer.Page? = null
                try {
                    page = renderer.openPage(index)
                    com.hitsuji.pdfdrivereader.domain.model.PageDimension(page.width, page.height)
                } finally {
                    page?.close()
                }
            }
            
            return PdfDocument(id = uri.toString(), fileName = fileName, totalPageCount = pageCount, pageSizes = sizes)
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
        activeId = null
        Log.d("PDFDriveReader", "PdfRenderer: Session closed")
    }
}
