package io.thoth.models

import io.ktor.server.routing.RoutingContext
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.openapi.common.Patch
import io.thoth.openapi.common.ifSet
import io.thoth.openapi.ktor.ValidateObject
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.database.tables.BookField
import org.jetbrains.exposed.v1.core.Op
import java.time.LocalDate
import java.util.UUID

data class BookUpdate(
    val title: Patch<String> = Patch.Absent,
    val authors: Patch<List<UUID>> = Patch.Absent,
    val series: Patch<List<UUID>?> = Patch.Absent,
    val provider: Patch<String?> = Patch.Absent,
    val providerID: Patch<String?> = Patch.Absent,
    val providerRating: Patch<Float?> = Patch.Absent,
    val releaseDate: Patch<LocalDate?> = Patch.Absent,
    val publisher: Patch<String?> = Patch.Absent,
    val language: Patch<MetadataLanguage?> = Patch.Absent,
    val description: Patch<String?> = Patch.Absent,
    val narrators: Patch<List<String>?> = Patch.Absent,
    val genres: Patch<List<String>?> = Patch.Absent,
    val isbn: Patch<String?> = Patch.Absent,
    val cover: Patch<String?> = Patch.Absent,
    val reset: Patch<List<BookField>> = Patch.Absent,
) : ValidateObject {
    override suspend fun RoutingContext.validateBody() {
        title.ifSet { if (it.isBlank()) throw ErrorResponse.userError("A book title cannot be empty") }
        authors.ifSet { if (it.isEmpty()) throw ErrorResponse.userError("A book must have at least one author") }
    }
}
