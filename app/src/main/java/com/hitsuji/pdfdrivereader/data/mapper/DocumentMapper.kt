package com.hitsuji.pdfdrivereader.data.mapper

import com.hitsuji.pdfdrivereader.data.local.entity.DocumentMetadataEntity
import com.hitsuji.pdfdrivereader.domain.model.DocumentMetadata
import com.hitsuji.pdfdrivereader.domain.model.SourceType

/**
 * Mapper responsible for converting between [DocumentMetadata] domain models 
 * and [DocumentMetadataEntity] data objects.
 */
class DocumentMapper {

    /**
     * Converts a domain [DocumentMetadata] into a [DocumentMetadataEntity].
     * 
     * @param domain The domain model to convert.
     * @return The resulting Room entity.
     */
    fun toEntity(domain: DocumentMetadata): DocumentMetadataEntity {
        return DocumentMetadataEntity(
            fileUri = domain.id,
            fileName = domain.fileName,
            locationPath = domain.locationPath,
            sourceType = domain.source.name,
            lastModified = System.currentTimeMillis()
        )
    }

    /**
     * Converts a [DocumentMetadataEntity] into a domain [DocumentMetadata].
     * 
     * @param entity The Room entity to convert.
     * @return The resulting domain model.
     */
    fun toDomain(entity: DocumentMetadataEntity): DocumentMetadata {
        return DocumentMetadata(
            id = entity.fileUri,
            fileName = entity.fileName,
            locationPath = entity.locationPath,
            source = SourceType.valueOf(entity.sourceType)
        )
    }
}
