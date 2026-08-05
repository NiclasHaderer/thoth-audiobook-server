package io.thoth.models

import io.ktor.server.routing.RoutingContext
import io.thoth.openapi.ktor.ValidateObject
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.util.UUID

data class SeriesUpdate(
    val title: String?,
    val authors: List<UUID>?,
    val books: List<UUID>?,
    val provider: String?,
    val providerID: String?,
    val totalBooks: Int?,
    val primaryWorks: Int?,
    val cover: String?,
    val description: String?,
) : ValidateObject {
    override suspend fun RoutingContext.validateBody() {
        if (authors?.isEmpty() == true) throw ErrorResponse.userError("A series must have at least one author")
        if (books?.isEmpty() == true) throw ErrorResponse.userError("A series must have at least one book")
    }
}
