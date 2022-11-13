package io.thoth.database.migrations.history

import io.thoth.database.migrations.migrator.Migration
import io.thoth.database.tables.meta.TMetaAuthorBookMapping
import io.thoth.database.tables.meta.TMetaAuthors
import io.thoth.database.tables.meta.TMetaBooks
import io.thoth.database.tables.meta.TMetaGenreAuthorMapping
import io.thoth.database.tables.meta.TMetaGenreBookMapping
import io.thoth.database.tables.meta.TMetaGenreSeriesMapping
import io.thoth.database.tables.meta.TMetaGenres
import io.thoth.database.tables.meta.TMetaSeries
import io.thoth.database.tables.meta.TMetaSeriesAuthorMapping
import io.thoth.database.tables.meta.TMetaSeriesBookMapping
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction


class `03_Create_Meta` : Migration() {
    override fun migrate(db: Database) {
        transaction {
            SchemaUtils.create(
                TMetaBooks,
                TMetaAuthors,
                TMetaSeries,
                TMetaGenres,
                TMetaAuthorBookMapping,
                TMetaGenreAuthorMapping,
                TMetaGenreBookMapping,
                TMetaGenreSeriesMapping,
                TMetaSeriesBookMapping,
                TMetaSeriesAuthorMapping
            )
        }
    }

    override fun rollback(db: Database) { // Nothing to do
    }

}
