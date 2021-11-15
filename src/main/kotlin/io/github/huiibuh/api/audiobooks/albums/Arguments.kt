package io.github.huiibuh.api.audiobooks.albums

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.*

@Path("{uuid}")
data class AlbumId(
    @PathParam("The id of the album you want to get") val uuid: UUID,
)
