package io.thoth.server.api.audiobooks.authors

import io.ktor.resources.*
import io.thoth.common.serializion.UUIDSerializer
import io.thoth.models.ProviderIDModel
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
@Resource("/{id}")
internal class AuthorId(
    @Serializable(UUIDSerializer::class) val uuid: UUID,
)

class PatchAuthor(
    val name: String,
    val biography: String?,
    val providerID: ProviderIDModel?,
    val image: String?,
)
