package io.thoth.server.database.migrations.history

import io.thoth.server.database.migrations.Migration
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.GenreBookTable
import io.thoth.server.database.tables.GenreSeriesTable
import io.thoth.server.database.tables.GenresTable
import io.thoth.server.database.tables.ImageTable
import io.thoth.server.database.tables.LibraryUserTable
import io.thoth.server.database.tables.SeriesAuthorTable
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.TracksTable
import io.thoth.server.database.tables.UsersTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class `01_INITIAL_SETUP` : Migration() {
    private val tables =
        listOf(
            AuthorTable,
            BooksTable,
            ImageTable,
            SeriesTable,
            GenresTable,
            TracksTable,
            UsersTable,
            AuthorBookTable,
            GenreBookTable,
            GenreSeriesTable,
            SeriesBookTable,
            SeriesAuthorTable,
            LibraryUserTable,
        ).toTypedArray()

    override fun migrate() {
        transaction { SchemaUtils.create(*tables) }
    }
}
