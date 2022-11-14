package io.thoth.database.migrations.history

import io.thoth.database.migrations.migrator.Migration
import io.thoth.database.tables.*
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime


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
