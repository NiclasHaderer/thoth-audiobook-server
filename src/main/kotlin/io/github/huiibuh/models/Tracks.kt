package io.github.huiibuh.models

import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TrackModel(
    @Serializable(UUIDSerializer::class) val id: UUID,
    val title: String,
    val trackNr: Int?,
    val duration: Int,
    val accessTime: Long,
    val book: TitledId,
    val path: String,
)
