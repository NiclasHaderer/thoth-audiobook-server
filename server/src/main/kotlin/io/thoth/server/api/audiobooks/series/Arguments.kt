package io.thoth.server.api.audiobooks.series

import io.ktor.resources.*
import io.thoth.common.serializion.kotlin.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
@Resource("{id}")
internal class SeriesId(
    @Serializable(UUIDSerializer::class) val id: UUID,
)


class PatchSeries(
    val title: String,
    val author: String?,
    val description: String?,
)
