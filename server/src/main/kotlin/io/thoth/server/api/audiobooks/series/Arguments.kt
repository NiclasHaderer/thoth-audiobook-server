package io.thoth.server.api.audiobooks.series

import io.ktor.resources.*
import io.thoth.common.serializion.kotlin.UUID_S
import kotlinx.serialization.Serializable
import java.util.*

@Resource("{id}")
internal class SeriesId(
    val id: UUID_S,
) {
    @Resource("position")
    class Position(val parent: SeriesId)
}

@Resource("")
internal class SeriesName(
    val name: String
)


@Serializable
data class PatchSeries(
    val title: String?,
    val authors: List<UUID_S>?,
    val books: List<UUID_S>?,
    val provider: String?,
    val providerID: String?,
    val totalBooks: Int?,
    val primaryWorks: Int?,
    val cover: String?,
    val description: String?
) {
    init {
        require(authors?.isNotEmpty() ?: true) { "Authors must not be empty" }
        require(books?.isNotEmpty() ?: true) { "Books must not be empty" }
    }
}

@Serializable
data class PostSeries(
    val title: String,
    val authors: List<UUID_S>,
    val books: List<UUID_S>,
    val provider: String?,
    val providerID: String?,
    val totalBooks: Int?,
    val primaryWorks: Int?,
    val cover: String?,
    val description: String?,
) {
    init {
        require(authors.isNotEmpty()) { "Authors must not be empty" }
        require(books.isNotEmpty()) { "Books must not be empty" }
    }
}