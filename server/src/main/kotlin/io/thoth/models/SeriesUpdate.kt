package io.thoth.models

import io.ktor.server.routing.RoutingContext
import io.thoth.openapi.common.Patch
import io.thoth.openapi.common.ifSet
import io.thoth.openapi.ktor.ValidateObject
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.util.UUID

data class SeriesUpdate(
    val title: Patch<String> = Patch.Absent,
    val books: Patch<List<UUID>> = Patch.Absent,
    val provider: Patch<String?> = Patch.Absent,
    val providerID: Patch<String?> = Patch.Absent,
    val totalBooks: Patch<Int?> = Patch.Absent,
    val primaryWorks: Patch<Int?> = Patch.Absent,
    val cover: Patch<String?> = Patch.Absent,
    val description: Patch<String?> = Patch.Absent,
) : ValidateObject {
    override suspend fun RoutingContext.validateBody() {
        title.ifSet { if (it.isBlank()) throw ErrorResponse.userError("A series title cannot be empty") }
        books.ifSet { if (it.isEmpty()) throw ErrorResponse.userError("A series must have at least one book") }
    }
}
