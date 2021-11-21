package db.migrations

import io.github.huiibuh.db.migration.Migration
import io.github.huiibuh.db.tables.*
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
                TSeries,
                TTracks,
            )
        }
    }

    override fun rollback(db: Database) {
        TODO("Not yet implemented")
    }

}
