package io.github.huiibuh.api.audiobooks

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import kotlinx.serialization.Serializable

@Path("{id}")
open class AudiobookId(
    @PathParam("The id of the audiobook") val id: String,
)

@Path("{id}")
@Serializable
open class Audiobook(
    @PathParam("The id of the audiobook") val id: String,
    val author: String? = null,
    val narrator: String? = null,
    val series: String? = null,
    val book: Int? = null,
)
