package io.github.huiibuh.api.audiobooks.authors

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.*

@Path("{uuid}")
internal data class AuthorId(
    @PathParam("The id of the author you want to get") val uuid: UUID,
)
