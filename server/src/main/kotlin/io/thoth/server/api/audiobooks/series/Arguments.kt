package io.thoth.server.api.audiobooks.series

import io.ktor.resources.*
import io.thoth.common.serializion.kotlin.UUID_S
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
@Resource("{id}")
internal class SeriesId(
    val id: UUID_S,
)


@Serializable
data class PatchSeries(
    val title: String?,
    val provider: String?,
    val providerID: String?,
    val totalBooks: Int?,
    val primaryWorks: Int?,
    val cover: String?,
    val description: String?
)

@Serializable
data class PostSeries(
    val title: String,
    val provider: String?,
    val providerID: String?,
    val totalBooks: Int?,
    val primaryWorks: Int?,
    val cover: String?,
    val description: String?,
)