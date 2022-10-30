package io.thoth.database.migrations.history

import io.thoth.database.migrations.migrator.Migration
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import java.util.*


class `02_Create_User` : Migration() {
    override fun migrate(db: Database) {
        transaction {
            SchemaUtils.create(
                TUsers
            )

            val encoder = Argon2PasswordEncoder()
            TUsers.insert {
                it[username] = "admin"
                it[passwordHash] = encoder.encode("admin")
                it[admin] = true
                it[edit] = true
                it[changePassword] = true
            }
            TUsers.insert {
                it[username] = "guest"
                it[passwordHash] = encoder.encode("guest")
                it[admin] = true
                it[edit] = true
                it[changePassword] = true
            }
        }
    }

    override fun rollback(db: Database) { // Nothing to do
    }

}

object TUsers : UUIDTable("Users") {
    val username = char("username", 256).uniqueIndex()
    val passwordHash = char("passwordHash", 512)
    val admin = bool("admin")
    val edit = bool("edit")
    val changePassword = bool("changePassword")
    val enabled = bool("enabled").default(true)
}

