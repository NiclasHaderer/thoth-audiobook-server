package io.thoth.models

import io.ktor.server.routing.RoutingContext
import io.thoth.openapi.ktor.ValidateObject
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.time.LocalDate
import java.util.UUID

data class BookUpdate(
    val title: String?,
    val authors: List<UUID>?,
    val series: List<UUID>?,
    val provider: String?,
    val providerID: String?,
    val providerRating: Float?,
    val releaseDate: LocalDate?,
    val publisher: String?,
    val language: String?,
    val description: String?,
    val narrators: List<String>?,
    val genres: List<String>?,
    val isbn: String?,
    val cover: String?,
) : ValidateObject {
    override suspend fun RoutingContext.validateBody() {
        if (authors?.isEmpty() == true) throw ErrorResponse.userError("A book must have at least one author")
    }
}
