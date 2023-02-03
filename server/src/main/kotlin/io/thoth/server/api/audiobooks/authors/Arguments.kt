package io.thoth.server.api.audiobooks.authors

import io.ktor.resources.*
import io.thoth.common.serializion.kotlin.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.*

@Serializable
@Resource("{id}")
internal class AuthorId(
    @Serializable(UUIDSerializer::class) val id: UUID,
)

class PatchAuthor(
    val name: String?,
    val provider: String?,
    val biography: String?,
    val imageID: UUID?,
    val website: String?,
    val bornIn: String?,
    val birthDate: LocalDate?,
    val deathDate: LocalDate?
)
