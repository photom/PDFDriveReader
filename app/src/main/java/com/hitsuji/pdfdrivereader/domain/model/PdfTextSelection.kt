package com.hitsuji.pdfdrivereader.domain.model

import android.graphics.PointF
import android.graphics.RectF

data class PdfTextSelection(
    val pageIndex: Int,
    val text: String,
    val bounds: List<RectF>,
    val selectionStart: PointF, // The raw user-initiated start point
    val selectionStop: PointF   // The raw user-initiated stop point
) {
    val startHandle: PointF
        get() = selectionStart

    val stopHandle: PointF
        get() = selectionStop
}