package io.thoth.models

import io.thoth.openapi.serializion.kotlin.UUID_S
import java.time.LocalDate

class BookUpdate(
    val title: String?,
    val authors: List<UUID_S>?,
    val series: List<UUID_S>?,
    val provider: String?,
    val providerID: String?,
    val providerRating: Float?,
    val releaseDate: LocalDate?,
    val publisher: String?,
    val language: String?,
    val description: String?,
    val narrator: String?,
    val isbn: String?,
    val cover: String?,
) {
    init {
        require(authors?.isNotEmpty() ?: true) { "Authors cannot be empty" }
    }
}
