package io.thoth.server.database.migrations.history

import io.thoth.server.database.migrations.migrator.Migration
import io.thoth.server.database.tables.TAuthorBookMapping
import io.thoth.server.database.tables.TAuthors
import io.thoth.server.database.tables.TBooks
import io.thoth.server.database.tables.TGenreBookMapping
import io.thoth.server.database.tables.TGenreSeriesMapping
import io.thoth.server.database.tables.TGenres
import io.thoth.server.database.tables.TImages
import io.thoth.server.database.tables.TSeries
import io.thoth.server.database.tables.TSeriesAuthorMapping
import io.thoth.server.database.tables.TSeriesBookMapping
import io.thoth.server.database.tables.TTracks
import io.thoth.server.database.tables.TUsers
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
                TTracks,
                TUsers,
                TAuthorBookMapping,
                TGenreBookMapping,
                TGenreSeriesMapping,
                TSeriesBookMapping,
                TSeriesAuthorMapping,
            )
            .toTypedArray()

    override fun migrate(db: Database) {
        transaction {
            SchemaUtils.create(*tables)
            val encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
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
        val tablesForDeletion = SchemaUtils.sortTablesByReferences(tables.toList()).reversed().filter { it in tables }
        return tablesForDeletion.flatMap { it.dropStatement() }
    }
}
