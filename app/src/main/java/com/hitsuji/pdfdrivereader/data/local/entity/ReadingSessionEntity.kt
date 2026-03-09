package com.hitsuji.pdfdrivereader.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a user's reading session for a specific document.
 */
@Entity(tableName = "READING_SESSION")
data class ReadingSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "file_uri")
    val fileUri: String,
    
    @ColumnInfo(name = "current_page")
    val currentPage: Int,
    
    @ColumnInfo(name = "reading_direction")
    val readingDirection: String,
    
    @ColumnInfo(name = "zoom_level")
    val zoomLevel: Float,

    @ColumnInfo(name = "cover_mode", defaultValue = "1")
    val coverMode: Boolean = true
)
