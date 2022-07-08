package io.thoth.server.api.audiobooks.series

import io.ktor.resources.*
import io.thoth.common.serializion.UUIDSerializer
import io.thoth.models.ProviderIDModel
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
@Resource("{id}")
internal class SeriesId(
    @Serializable(UUIDSerializer::class) val id: UUID,
)


class PatchSeries(
    val title: String,
    val providerID: ProviderIDModel?,
    val author: String?,
    val description: String?,
)
