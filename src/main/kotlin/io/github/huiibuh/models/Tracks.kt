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
    @Serializable(UUIDSerializer::class) val book: UUID,
    @Serializable(UUIDSerializer::class) val author: UUID,
    @Serializable(UUIDSerializer::class) val narrator: UUID?,
    @Serializable(UUIDSerializer::class) val series: UUID?,
    val seriesIndex: Int?,
)
