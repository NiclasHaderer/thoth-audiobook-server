package io.github.huiibuh.api.audiobooks.books

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.*

@Path("{uuid}")
internal data class BookId(
    @PathParam("The id of the book you want to get") val uuid: UUID,
)
