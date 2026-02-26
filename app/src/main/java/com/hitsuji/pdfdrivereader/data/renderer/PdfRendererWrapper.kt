package com.hitsuji.pdfdrivereader.data.renderer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import com.hitsuji.pdfdrivereader.domain.model.PdfDocument
import java.io.File

/**
 * Android-specific wrapper for the native [PdfRenderer].
 * 
 * Handles the opening of [ParcelFileDescriptor] and ensures resources are 
 * properly closed after use.
 */
class PdfRendererWrapper {

    /**
     * Opens a PDF file and returns its metadata.
     * 
     * @param file The local [File] to open.
     * @return A [PdfDocument] containing the page count and URI.
     */
    fun openDocument(file: File): PdfDocument {
        Log.d("PDFDriveReader", "PdfRenderer: Opening document ${file.absolutePath}")
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            return PdfDocument(
                id = file.absolutePath,
                totalPageCount = pageCount
            )
        } catch (e: Exception) {
            Log.e("PDFDriveReader", "PdfRenderer: Error opening document", e)
            throw e
        } finally {
            renderer?.close()
            pfd?.close()
        }
    }

    /**
     * Retrieves the original size of a specific page in points (1/72 inch).
     * 
     * @param file The PDF file.
     * @param pageIndex The 0-based page index.
     * @return A Pair of (Width, Height).
     */
    fun getPageSize(file: File, pageIndex: Int): Pair<Int, Int> {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            page = renderer.openPage(pageIndex)
            return Pair(page.width, page.height)
        } finally {
            page?.close()
            renderer?.close()
            pfd?.close()
        }
    }

    /**
     * Renders a specific page to a [Bitmap].
     * 
     * @param file The PDF file.
     * @param pageIndex The 0-based page index.
     * @param width The target bitmap width.
     * @param height The target bitmap height.
     * @return A [Bitmap] of the rendered page.
     */
    fun renderPage(file: File, pageIndex: Int, width: Int, height: Int): Bitmap {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            page = renderer.openPage(pageIndex)
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        } catch (e: Exception) {
            Log.e("PDFDriveReader", "PdfRenderer: Error rendering page $pageIndex", e)
            throw e
        } finally {
            page?.close()
            renderer?.close()
            pfd?.close()
        }
    }
}
