package io.thoth.database.migrations.history

import io.thoth.database.migrations.migrator.Migration
import io.thoth.database.tables.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction


class `01_Create_Tables` : Migration() {
    override fun migrate(db: Database) {
        transaction {
            SchemaUtils.create(
                TAuthors,
                TBooks,
                TImages,
                TSeries,
                TKeyValueSettings,
                TTracks,
            )
        }
    }

    override fun rollback(db: Database) { // Nothing to do
    }

}
