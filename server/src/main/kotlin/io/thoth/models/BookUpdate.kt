package io.thoth.models

import io.ktor.server.routing.RoutingContext
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.openapi.ktor.ValidateObject
import io.thoth.openapi.ktor.errors.ErrorResponse
import org.jetbrains.exposed.v1.core.Op
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

data class BookUpdate(
    val title: Optional<String> = Optional.empty(),
    val authors: Optional<List<UUID>> = Optional.empty(),
    val series: Optional<List<UUID>>? = Optional.empty(),
    val provider: Optional<String>? = Optional.empty(),
    val providerID: Optional<String>? = Optional.empty(),
    val providerRating: Optional<Float>? = Optional.empty(),
    val releaseDate: Optional<LocalDate>? = Optional.empty(),
    val publisher: Optional<String>? = Optional.empty(),
    val language: Optional<MetadataLanguage>? = Optional.empty(),
    val description: Optional<String>? = Optional.empty(),
    val narrators: Optional<List<String>>? = Optional.empty(),
    val genres: Optional<List<String>>? = Optional.empty(),
    val isbn: Optional<String>? = Optional.empty(),
    val cover: Optional<String>? = Optional.empty(),
) : ValidateObject {
    override suspend fun RoutingContext.validateBody() {
        if (title.getOrNull()?.isBlank() == true) throw ErrorResponse.userError("A book title cannot be empty")
        if (authors.getOrNull()?.isEmpty() == true) {
            throw ErrorResponse.userError("A book must have at least one author")
        }
    }
}
