package io.thoth.models

import io.ktor.server.routing.RoutingContext
import io.thoth.openapi.common.Patch
import io.thoth.openapi.common.ifSet
import io.thoth.openapi.ktor.ValidateObject
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.database.tables.AuthorField
import java.time.LocalDate
import java.util.UUID

data class AuthorUpdate(
    val name: Patch<String> = Patch.Absent,
    val provider: Patch<String?> = Patch.Absent,
    val providerID: Patch<String?> = Patch.Absent,
    val biography: Patch<String?> = Patch.Absent,
    val image: Patch<String?> = Patch.Absent,
    val website: Patch<String?> = Patch.Absent,
    val bornIn: Patch<String?> = Patch.Absent,
    val birthDate: Patch<LocalDate?> = Patch.Absent,
    val deathDate: Patch<LocalDate?> = Patch.Absent,
    val books: Patch<List<UUID>> = Patch.Absent,
    val reset: Patch<List<AuthorField>> = Patch.Absent,
) : ValidateObject {
    override suspend fun RoutingContext.validateBody() {
        name.ifSet { if (it.isBlank()) throw ErrorResponse.userError("An author name cannot be empty") }
        books.ifSet { if (it.isEmpty()) throw ErrorResponse.userError("An author must have at least one book") }
    }
}
