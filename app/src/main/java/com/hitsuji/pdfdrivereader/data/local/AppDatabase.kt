package com.hitsuji.pdfdrivereader.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hitsuji.pdfdrivereader.data.local.dao.PdfDao
import com.hitsuji.pdfdrivereader.data.local.entity.DocumentMetadataEntity
import com.hitsuji.pdfdrivereader.data.local.entity.NavHistoryEntity
import com.hitsuji.pdfdrivereader.data.local.entity.ReadingSessionEntity

/**
 * Primary Room database for the PDFDriveReader application.
 */
@Database(
    entities = [
        DocumentMetadataEntity::class,
        ReadingSessionEntity::class,
        NavHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    /**
     * @return The [PdfDao] for database operations.
     */
    abstract fun pdfDao(): PdfDao
}
