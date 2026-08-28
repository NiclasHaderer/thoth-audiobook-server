package io.thoth.server.repositories

import io.thoth.models.Genre
import io.thoth.models.GenreDetailed
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.database.views.BookMetadataView
import io.thoth.server.database.views.booksToModels
import io.thoth.server.database.views.bookGroupCount
import io.thoth.server.database.views.bookGroups
import io.thoth.server.database.views.booksInGroup
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

interface GenreRepository {
    fun getAll(
        libraryId: UUID,
        order: SortOrder,
        limit: Int = 20,
        offset: Long = 0L,
    ): List<Genre>

    fun get(
        userId: UUID,
        name: String,
        libraryId: UUID,
    ): GenreDetailed

    fun total(libraryId: UUID): Long
}

class GenreRepositoryImpl : GenreRepository {
    override fun getAll(
        libraryId: UUID,
        order: SortOrder,
        limit: Int,
        offset: Long,
    ): List<Genre> =
        transaction {
            BookMetadataView.genres
                .bookGroups(libraryId, order, limit, offset)
                .map { Genre(name = it.name, bookCount = it.bookCount) }
        }

    override fun get(
        userId: UUID,
        name: String,
        libraryId: UUID,
    ): GenreDetailed =
        transaction {
            val match =
                BookMetadataView.genres.booksInGroup(name, libraryId)
                    ?: throw ErrorResponse.notFound("Genre", name)
            GenreDetailed(name = match.name, books = booksToModels(match.books, userId))
        }

    override fun total(libraryId: UUID): Long = transaction { BookMetadataView.genres.bookGroupCount(libraryId) }
}
