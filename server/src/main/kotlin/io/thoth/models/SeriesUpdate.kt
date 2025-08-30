package io.thoth.models

import io.thoth.openapi.serializion.kotlin.UUID_S

data class SeriesUpdate(
    val title: String?,
    val authors: List<UUID_S>?,
    val books: List<UUID_S>?,
    val provider: String?,
    val providerID: String?,
    val totalBooks: Int?,
    val primaryWorks: Int?,
    val cover: String?,
    val description: String?,
) {
    init {
        require(authors?.isNotEmpty() ?: true) { "Authors must not be empty" }
        require(books?.isNotEmpty() ?: true) { "Books must not be empty" }
    }
}
