package io.github.huiibuh.api.audiobooks.artists

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.*

@Path("{uuid}")
data class ArtistId(
    @PathParam("The id of the artist you want to get") val uuid: UUID,
)
