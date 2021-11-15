package io.github.huiibuh.api.audiobooks.collections

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.parameters.QueryParamStyle
import java.util.*

@Path("{uuid}")
data class CollectionId(
    @PathParam("The id of the collection you want to get") val uuid: UUID,
)
@Path("{uuid}/albums")
data class CollectionAlbums(
    @PathParam("The id of the collection you want to get") val uuid: UUID,
)
