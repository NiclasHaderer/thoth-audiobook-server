package io.github.huiibuh.api.audiobooks.books

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Path("{uuid}")
internal data class BookId(
    @PathParam("The id of the book you want to get") val uuid: UUID,
)


internal data class PatchBook(
    val title: String?,
    val language: String?,
    val description: String?,
    val asin: String?,
    val author: String?,
    val narrator: String?,
    val series: String?,
    val seriesIndex: Int?,
    val cover: String?,
)
