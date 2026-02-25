package com.hitsuji.pdfdrivereader.data.renderer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
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
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount
        renderer.close()
        pfd.close()
        
        return PdfDocument(
            id = file.absolutePath,
            totalPageCount = pageCount
        )
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
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val page = renderer.openPage(pageIndex)
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        
        page.close()
        renderer.close()
        pfd.close()
        
        return bitmap
    }
}
