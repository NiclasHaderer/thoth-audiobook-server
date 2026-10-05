package io.thoth.models

import io.ktor.server.routing.RoutingContext
import io.thoth.openapi.ktor.ValidateObject
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

data class AuthorUpdate(
    val name: Optional<String> = Optional.empty(),
    val provider: Optional<String>? = Optional.empty(),
    val providerID: Optional<String>? = Optional.empty(),
    val biography: Optional<String>? = Optional.empty(),
    val image: Optional<String>? = Optional.empty(),
    val website: Optional<String>? = Optional.empty(),
    val bornIn: Optional<String>? = Optional.empty(),
    val birthDate: Optional<LocalDate>? = Optional.empty(),
    val deathDate: Optional<LocalDate>? = Optional.empty(),
    val books: Optional<List<UUID>> = Optional.empty(),
) : ValidateObject {
    override suspend fun RoutingContext.validateBody() {
        if (name.getOrNull()?.isBlank() == true) throw ErrorResponse.userError("An author name cannot be empty")
        if (books.getOrNull()?.isEmpty() == true) throw ErrorResponse.userError("An author must have at least one book")
    }
}
