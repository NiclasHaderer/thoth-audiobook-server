package io.thoth.server.api.audiobooks.authors

import io.ktor.resources.*
import io.thoth.common.serializion.kotlin.UUID_S
import io.thoth.server.api.audiobooks.books.BookId
import java.time.LocalDate
import java.util.*

@Resource("{id}")
internal class AuthorId(
    val id: UUID_S,
) {

  @Resource("position") class Position(val parent: BookId)
}

@Resource("") internal class AuthorName(val name: String)

class PatchAuthor(
    val name: String?,
    val provider: String?,
    val providerID: String?,
    val biography: String?,
    val image: String?,
    val website: String?,
    val bornIn: String?,
    val birthDate: LocalDate?,
    val deathDate: LocalDate?
)

class PostAuthor(
    val name: String,
    val provider: String?,
    val providerID: String?,
    val biography: String?,
    val image: String?,
    val website: String?,
    val bornIn: String?,
    val birthDate: LocalDate?,
    val deathDate: LocalDate?
)
