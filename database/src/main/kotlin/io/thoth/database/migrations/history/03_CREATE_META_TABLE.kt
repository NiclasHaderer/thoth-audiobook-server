package io.thoth.database.migrations.history

import io.thoth.database.migrations.migrator.Migration
import io.thoth.database.tables.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction


class `03_Create_Meta` : Migration() {
    override fun migrate(db: Database) {
        transaction {
            SchemaUtils.create(
                TBooks,
                TAuthors,
                TSeries,
                TGenres,
                TAuthorBookMapping,
                TGenreAuthorMapping,
                TGenreBookMapping,
                TGenreSeriesMapping,
                TSeriesBookMapping,
                TSeriesAuthorMapping
            )
        }
    }

    override fun rollback(db: Database) { // Nothing to do
    }

}
