package com.hitsuji.pdfdrivereader.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room entity representing an entry in the navigation history stack for a document.
 */
@Entity(
    tableName = "NAV_HISTORY",
    foreignKeys = [
        ForeignKey(
            entity = DocumentMetadataEntity::class,
            parentColumns = ["file_uri"],
            childColumns = ["file_uri"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["file_uri"])]
)
data class NavHistoryEntity(
    @ColumnInfo(name = "id")
    @androidx.room.PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "file_uri")
    val fileUri: String,
    
    @ColumnInfo(name = "page_index")
    val pageIndex: Int,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)
