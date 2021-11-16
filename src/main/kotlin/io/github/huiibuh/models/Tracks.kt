package io.github.huiibuh.models

import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

data class TrackModel(
    @Serializable(UUIDSerializer::class) val id: UUID,
    val title: String,
    val trackNr: Int?,
    val duration: Int,
    val accessTime: Long,
    @Serializable(UUIDSerializer::class) val album: UUID,
    @Serializable(UUIDSerializer::class) val artist: UUID,
    @Serializable(UUIDSerializer::class) val composer: UUID?,
    @Serializable(UUIDSerializer::class) val collection: UUID?,
    val collectionIndex: Int?,
)
