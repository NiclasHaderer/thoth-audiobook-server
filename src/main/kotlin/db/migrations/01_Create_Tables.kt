package db.migrations

import io.github.huiibuh.db.migration.Migration
import io.github.huiibuh.db.tables.TAuthors
import io.github.huiibuh.db.tables.TBooks
import io.github.huiibuh.db.tables.TImages
import io.github.huiibuh.db.tables.TProviderID
import io.github.huiibuh.db.tables.TSeries
import io.github.huiibuh.db.tables.TSharedSettings
import io.github.huiibuh.db.tables.TTracks
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * TODO create real migration...
 */
class `01_Create_Tables` : Migration() {
    override fun migrate(db: Database) {
        transaction {
            SchemaUtils.create(
                TAuthors,
                TBooks,
                TImages,
                TProviderID,
                TSeries,
                TSharedSettings,
                TTracks,
            )
        }
    }

    override fun rollback(db: Database) {
        TODO("Not yet implemented")
    }

}
