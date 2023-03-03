package io.thoth.database.migrations.history

import io.thoth.database.migrations.migrator.Migration
import io.thoth.database.tables.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder

class `01_Create_Tables` : Migration() {
    private val tables =
        listOf(
                TAuthors,
                TBooks,
                TImages,
                TSeries,
                TGenres,
                TKeyValueSettings,
                TTracks,
                TUsers,
                TAuthorBookMapping,
                TGenreBookMapping,
                TGenreSeriesMapping,
                TSeriesBookMapping,
                TSeriesAuthorMapping
            )
            .toTypedArray()

    override fun migrate(db: Database) {
        transaction {
            SchemaUtils.create(*tables)
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

    override fun generateRollbackStatements(db: Database): List<String> {
        val tablesForDeletion =
            SchemaUtils.sortTablesByReferences(tables.toList()).reversed().filter { it in tables }
        return tablesForDeletion.flatMap { it.dropStatement() }
    }
}
