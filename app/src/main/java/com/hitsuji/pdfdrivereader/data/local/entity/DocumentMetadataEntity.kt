package com.hitsuji.pdfdrivereader.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a PDF document in the local cache.
 */
@Entity(tableName = "DOCUMENT_METADATA")
data class DocumentMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "file_uri")
    val fileUri: String,
    
    @ColumnInfo(name = "file_name")
    val fileName: String,
    
    @ColumnInfo(name = "location_path")
    val locationPath: String,
    
    @ColumnInfo(name = "source_type")
    val sourceType: String,
    
    @ColumnInfo(name = "last_modified")
    val lastModified: Long
)
