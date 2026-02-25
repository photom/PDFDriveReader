package com.hitsuji.pdfdrivereader.data.mapper

import com.hitsuji.pdfdrivereader.data.local.entity.ReadingSessionEntity
import com.hitsuji.pdfdrivereader.domain.model.PagePosition
import com.hitsuji.pdfdrivereader.domain.model.ReadingDirection
import com.hitsuji.pdfdrivereader.domain.model.ReadingSettings

/**
 * Mapper responsible for converting between reading session data entities and domain objects.
 */
class ReadingSessionMapper {

    /**
     * Converts domain position and settings into a [ReadingSessionEntity].
     * 
     * @param fileUri The unique identifier of the file.
     * @param position The current page position.
     * @param settings The user's reading preferences.
     * @return The resulting Room entity.
     */
    fun toEntity(fileUri: String, position: PagePosition, settings: ReadingSettings): ReadingSessionEntity {
        return ReadingSessionEntity(
            fileUri = fileUri,
            currentPage = position.pageIndex,
            readingDirection = settings.direction.name,
            zoomLevel = position.zoomLevel
        )
    }

    /**
     * Extracts domain models from a [ReadingSessionEntity].
     * 
     * @param entity The Room entity to convert.
     * @return A Pair containing the [PagePosition] and [ReadingSettings].
     */
    fun toDomain(entity: ReadingSessionEntity): Pair<PagePosition, ReadingSettings> {
        val position = PagePosition(entity.currentPage, entity.zoomLevel)
        val settings = ReadingSettings(
            direction = ReadingDirection.valueOf(entity.readingDirection),
            savedZoom = entity.zoomLevel
        )
        return position to settings
    }
}
