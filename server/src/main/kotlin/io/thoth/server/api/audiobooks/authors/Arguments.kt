package io.thoth.server.api.audiobooks.authors

import io.ktor.resources.*
import io.thoth.common.serializion.kotlin.UUID_S
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.*

@Serializable
@Resource("{id}")
internal class AuthorId(
    val id: UUID_S,
)

class PatchAuthor(
    val name: String?,
    val provider: String?,
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
    val biography: String?,
    val image: String?,
    val website: String?,
    val bornIn: String?,
    val birthDate: LocalDate?,
    val deathDate: LocalDate?
)