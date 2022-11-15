package io.thoth.server.api.audiobooks.authors

import io.ktor.resources.*
import io.thoth.common.serializion.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
@Resource("{id}")
internal class AuthorId(
    @Serializable(UUIDSerializer::class) val id: UUID,
)

class PatchAuthor(
    val name: String,
    val biography: String?,
    val image: String?,
)
