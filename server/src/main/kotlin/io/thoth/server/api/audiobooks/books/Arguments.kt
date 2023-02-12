package io.thoth.server.api.audiobooks.books

import io.ktor.resources.*
import io.thoth.common.serializion.kotlin.UUID_S
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.*

@Serializable
@Resource("{id}")
internal class BookId(
    val id: UUID_S,
)


class PatchBook(
    val title: String?,
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
)

class PostBook(
    val title: String,
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
)