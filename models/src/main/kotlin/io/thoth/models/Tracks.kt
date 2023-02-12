package io.thoth.models

import io.thoth.common.serializion.kotlin.LocalDateTime_S
import io.thoth.common.serializion.kotlin.UUID_S
import kotlinx.serialization.Serializable

@Serializable
data class TrackModel(
    val id: UUID_S,
    val title: String,
    val trackNr: Int?,
    val duration: Int,
    val accessTime: Long,
    val book: TitledId,
    val path: String,
    val updateTime: LocalDateTime_S,
)
