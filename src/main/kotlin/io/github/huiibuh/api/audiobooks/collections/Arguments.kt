package io.github.huiibuh.api.audiobooks.collections

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.*

@Path("{id}")
data class CollectionId(
    @PathParam("The id of the collection you want to get") val id: UUID,
)
