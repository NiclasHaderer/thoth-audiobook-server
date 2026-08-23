package io.thoth.models

import io.ktor.server.routing.RoutingContext
import io.thoth.openapi.ktor.ValidateObject
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.time.LocalDate
import java.util.UUID

data class AuthorUpdate(
    val name: String?,
    val provider: String?,
    val providerID: String?,
    val biography: String?,
    val image: String?,
    val website: String?,
    val bornIn: String?,
    val birthDate: LocalDate?,
    val deathDate: LocalDate?,
    val books: List<UUID>?,
) : ValidateObject {
    override suspend fun RoutingContext.validateBody() {
        if (books?.isEmpty() == true) throw ErrorResponse.userError("An author must have at least one book")
    }
}
