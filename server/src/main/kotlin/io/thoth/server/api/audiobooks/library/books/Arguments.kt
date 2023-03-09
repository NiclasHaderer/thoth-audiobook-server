package io.thoth.server.api.audiobooks.library.books

import io.ktor.resources.*
import io.thoth.common.serializion.kotlin.UUID_S
import java.time.LocalDate
import java.util.*

@Resource("{id}")
data class BookId(
    val id: UUID_S,
) {
    @Resource("position") data class Position(val route: BookId)
}

@Resource("") data class BookName(val name: String)

class PatchBook(
    val title: String?,
    val authors: List<UUID_S>?,
    val series: List<UUID_S>?,
    val provider: String?,
    val providerID: String?,
    val providerRating: Float?,
    val releaseDate: LocalDate?,
    val publisher: String?,
    val language: String?,
    val description: String?,
    val narrator: String?,
    val isbn: String?,
    val cover: String?
) {
    init {
        require(authors?.isNotEmpty() ?: true) { "Authors cannot be empty" }
    }
}

class PostBook(
    val title: String,
    val authors: List<UUID_S>,
    val series: List<UUID_S>?,
    val provider: String?,
    val providerID: String?,
    val providerRating: Float?,
    val releaseDate: LocalDate?,
    val publisher: String?,
    val language: String?,
    val description: String?,
    val narrator: String?,
    val isbn: String?,
    val cover: String?,
) {
    init {
        require(authors.isNotEmpty()) { "Authors cannot be empty" }
    }
}
