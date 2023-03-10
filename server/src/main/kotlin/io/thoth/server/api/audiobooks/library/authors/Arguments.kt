package io.thoth.server.api.audiobooks.library.authors

import io.ktor.resources.*
import io.thoth.common.serializion.kotlin.UUID_S
import java.time.LocalDate
import java.util.*

@Resource("{id}")
data class AuthorId(
    val id: UUID_S,
) {
    @Resource("position") data class Position(val parent: AuthorId)
}

@Resource("") data class AuthorName(val name: String)

data class PatchAuthor(
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

data class PostAuthor(
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
