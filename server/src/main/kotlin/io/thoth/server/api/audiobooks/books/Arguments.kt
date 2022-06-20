package io.thoth.server.api.audiobooks.books

import io.ktor.resources.*
import io.thoth.common.serializion.UUIDSerializer
import io.thoth.models.ProviderIDModel
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
@Resource("/{id}")
internal class BookId(
    @Serializable(UUIDSerializer::class) val id: UUID,
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
