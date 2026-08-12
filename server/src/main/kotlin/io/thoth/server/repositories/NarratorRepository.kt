package io.thoth.server.repositories

import io.thoth.models.Narrator
import io.thoth.models.NarratorDetailed
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.database.views.BookMetadataView
import io.thoth.server.database.views.booksToModels
import io.thoth.server.database.views.bookGroupCount
import io.thoth.server.database.views.bookGroups
import io.thoth.server.database.views.booksInGroup
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

interface NarratorRepository {
    fun getAll(
        libraryId: UUID,
        order: SortOrder,
        limit: Int = 20,
        offset: Long = 0L,
    ): List<Narrator>

    fun get(
        name: String,
        libraryId: UUID,
    ): NarratorDetailed

    fun total(libraryId: UUID): Long
}

class NarratorRepositoryImpl : NarratorRepository {
    override fun getAll(
        libraryId: UUID,
        order: SortOrder,
        limit: Int,
        offset: Long,
    ): List<Narrator> =
        transaction {
            BookMetadataView.narrators
                .bookGroups(libraryId, order, limit, offset)
                .map { Narrator(name = it.name, bookCount = it.bookCount) }
        }

    override fun get(
        name: String,
        libraryId: UUID,
    ): NarratorDetailed =
        transaction {
            val match =
                BookMetadataView.narrators.booksInGroup(name, libraryId)
                    ?: throw ErrorResponse.notFound("Narrator", name)
            NarratorDetailed(name = match.name, books = booksToModels(match.books))
        }

    override fun total(libraryId: UUID): Long =
        transaction { BookMetadataView.narrators.bookGroupCount(libraryId) }
}
