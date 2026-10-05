package io.thoth.models

import io.ktor.server.routing.RoutingContext
import io.thoth.openapi.ktor.ValidateObject
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.util.Optional
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

data class SeriesUpdate(
    val title: Optional<String> = Optional.empty(),
    val books: Optional<List<UUID>> = Optional.empty(),
    val provider: Optional<String>? = Optional.empty(),
    val providerID: Optional<String>? = Optional.empty(),
    val totalBooks: Optional<Int>? = Optional.empty(),
    val primaryWorks: Optional<Int>? = Optional.empty(),
    val cover: Optional<String>? = Optional.empty(),
    val description: Optional<String>? = Optional.empty(),
) : ValidateObject {
    override suspend fun RoutingContext.validateBody() {
        if (title.getOrNull()?.isBlank() == true) throw ErrorResponse.userError("A series title cannot be empty")
        if (books.getOrNull()?.isEmpty() == true) throw ErrorResponse.userError("A series must have at least one book")
    }
}
