package com.hitsuji.pdfdrivereader.domain.model

import android.graphics.PointF
import android.graphics.RectF

data class PdfTextSelection(
    val text: String,
    val bounds: List<RectF>
) {
    val startHandle: PointF?
        get() = bounds.firstOrNull()?.let { PointF(it.left, it.top) } // Top-left of the first rect as start handle base

    val stopHandle: PointF?
        get() = bounds.lastOrNull()?.let { PointF(it.right, it.bottom) } // Bottom-right of the last rect as stop handle base
}