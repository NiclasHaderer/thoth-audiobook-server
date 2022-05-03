package io.thoth.server.api.audiobooks.authors

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import io.thoth.models.ProviderIDModel
import java.util.*

@Path("{uuid}")
internal class AuthorId(
    @PathParam("The id of the author you want to get") val uuid: UUID,
)

class PatchAuthor(
    val name: String,
    val biography: String?,
    val providerID: ProviderIDModel?,
    val image: String?,
)
