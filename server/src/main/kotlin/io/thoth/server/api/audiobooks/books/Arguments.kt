package io.thoth.server.api.audiobooks.books

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import io.thoth.models.ProviderIDModel
import java.util.*

@Path("{uuid}")
internal class BookId(
    @PathParam("The id of the book you want to get") val uuid: UUID,
)


class PatchBook(
    val title: String,
    val language: String?,
    val description: String?,
    val providerID: ProviderIDModel?,
    val author: String,
    val narrator: String?,
    val series: String?,
    val seriesIndex: Float?,
    val cover: String?,
    val year: Int?,
)
