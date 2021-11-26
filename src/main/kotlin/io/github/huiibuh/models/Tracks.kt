package io.github.huiibuh.models

import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TrackModel(
    @Serializable(UUIDSerializer::class) val id: UUID,
    val title: String,
    @Serializable(UUIDSerializer::class) val cover: UUID?,
    val trackNr: Int?,
    val duration: Int,
    val accessTime: Long,
    val book: TitledId,
    val author: NamedId,
    val narrator: NamedId?,
    val series: TitledId?,
    val seriesIndex: Int?,
)
