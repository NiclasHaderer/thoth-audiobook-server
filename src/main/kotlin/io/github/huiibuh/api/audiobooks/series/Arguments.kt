package io.github.huiibuh.api.audiobooks.series

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Path("{uuid}")
internal data class SeriesId(
    @PathParam("The id of the series you want to get") val uuid: UUID,
)


data class PatchSeries(
    val title: String?,
    val asin: String?,
    val description: String?,
)
