package io.github.huiibuh.api.audiobooks.series

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.*

@Path("{uuid}")
internal data class SeriesId(
    @PathParam("The id of the series you want to get") val uuid: UUID,
)
