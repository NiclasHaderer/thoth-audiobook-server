package io.github.huiibuh.models

import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*


@Serializable
data class AlbumModel(
    @Serializable(UUIDSerializer::class) val value: UUID,
    val title: String,
    val language: String?,
    val description: String?,
    val asin: String?,
    @Serializable(UUIDSerializer::class) val artist: UUID,
    @Serializable(UUIDSerializer::class) val composer: UUID?,
    @Serializable(UUIDSerializer::class) val collection: UUID?,
    val collectionIndex: Int?,
    @Serializable(UUIDSerializer::class) val cover: UUID?,
)

@Serializable
data class AlbumWithTracks(
    @Serializable(UUIDSerializer::class) val value: UUID,
    val title: String,
    val description: String?,
    val asin: String?,
    @Serializable(UUIDSerializer::class) val artist: UUID,
    @Serializable(UUIDSerializer::class) val composer: UUID?,
    @Serializable(UUIDSerializer::class) val collection: UUID?,
    val collectionIndex: Int?,
    @Serializable(UUIDSerializer::class) val cover: UUID?,
)
