package io.thoth.models

import io.thoth.common.serializion.kotlin.LocalDateTimeSerializer
import io.thoth.common.serializion.kotlin.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
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
    @Serializable(LocalDateTimeSerializer::class) val updateTime: LocalDateTime,
)
