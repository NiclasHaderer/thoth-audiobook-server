package io.thoth.database.migrations.history

import io.thoth.database.migrations.migrator.Migration
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction


class `02_Create_User` : Migration() {
    override fun migrate(db: Database) {
        transaction {
            SchemaUtils.create(
                TUsers
            )
        }
    }

    override fun rollback(db: Database) { // Nothing to do
    }

}

object TUsers : UUIDTable("Users") {
    val username = char("username", 256).uniqueIndex()
    val passwordHash = char("passwordHash", 512)
    val admin = bool("admin").default(false)
    val edit = bool("edit").default(false)
}

