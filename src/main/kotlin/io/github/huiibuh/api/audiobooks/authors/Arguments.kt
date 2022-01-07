package io.github.huiibuh.api.audiobooks.authors

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import io.github.huiibuh.models.ProviderIDModel
import java.util.*

@Path("{uuid}")
internal data class AuthorId(
    @PathParam("The id of the author you want to get") val uuid: UUID,
)

internal data class PatchAuthor(
    val name: String,
    val biography: String?,
    val providerID: ProviderIDModel,
    val image: String?,
)
