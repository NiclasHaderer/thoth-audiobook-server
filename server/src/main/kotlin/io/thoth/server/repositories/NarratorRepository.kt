package io.thoth.server.repositories

import io.thoth.models.Narrator
import io.thoth.models.NarratorDetailed
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.database.rows.bookGroupCount
import io.thoth.server.database.rows.bookGroups
import io.thoth.server.database.rows.booksInGroup
import io.thoth.server.database.rows.booksToModels
import io.thoth.server.database.tables.BooksTable
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
        userId: UUID,
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
            BooksTable.narrators
                .bookGroups(libraryId, order, limit, offset)
                .map { Narrator(name = it.name, bookCount = it.bookCount) }
        }

    override fun get(
        userId: UUID,
        name: String,
        libraryId: UUID,
    ): NarratorDetailed =
        transaction {
            val match =
                BooksTable.narrators.booksInGroup(name, libraryId)
                    ?: throw ErrorResponse.notFound("Narrator", name)
            NarratorDetailed(name = match.name, books = booksToModels(match.books, userId))
        }

    override fun total(libraryId: UUID): Long = transaction { BooksTable.narrators.bookGroupCount(libraryId) }
}
