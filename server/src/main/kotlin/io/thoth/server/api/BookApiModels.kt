package io.thoth.server.api

import io.thoth.server.common.serializion.kotlin.UUID_S
import java.time.LocalDate

class PartialBookApiModel(
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
    val cover: String?
) {
    init {
        require(authors?.isNotEmpty() ?: true) { "Authors cannot be empty" }
    }
}

class BookApiModel(
    val title: String,
    val authors: List<UUID_S>,
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
        require(authors.isNotEmpty()) { "Authors cannot be empty" }
    }
}
