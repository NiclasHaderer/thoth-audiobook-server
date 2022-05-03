package io.thoth.server.api.audiobooks.series

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import io.thoth.models.ProviderIDModel
import java.util.*

@Path("{uuid}")
internal class SeriesId(
    @PathParam("The id of the series you want to get") val uuid: UUID,
)


class PatchSeries(
    val title: String,
    val providerID: ProviderIDModel?,
    val author: String?,
    val description: String?,
)
