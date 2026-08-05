package io.thoth.server.database.migrations

import io.thoth.server.ThothTest
import io.thoth.server.database.tables.UsersTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DatabaseMigratorTest : ThothTest(migrate = false) {
    private fun appliedVersions(): List<Int> = transaction { SchemaTrackerEntity.all().map { it.version }.sorted() }

    private fun tableNames(): List<String> =
        transaction {
            exec("SELECT name FROM sqlite_master WHERE type='table'") { rs ->
                buildList { while (rs.next()) add(rs.getString(1)) }
            }!!
        }

    @Test
    fun `fresh database gets all migrations applied`() {
        val migrator = DatabaseMigrator()
        migrator.migrateDatabase()

        assertEquals(migrator.knownVersions, appliedVersions())
        val tables = tableNames()
        for (table in listOf(
            "Libraries", "Authors", "Books", "Images", "Series", "Genres", "Tracks", "Users",
            "AuthorBook", "GenreBook", "GenreSeries", "SeriesBook", "SeriesAuthor", "LibraryUser", "SchemaTracker",
        )) {
            assertTrue(table in tables, "Table $table missing, got $tables")
        }
    }

    @Test
    fun `rerunning on an up-to-date database changes nothing`() {
        val migrator = DatabaseMigrator()
        migrator.migrateDatabase()
        DatabaseMigrator().migrateDatabase()

        assertEquals(migrator.knownVersions, appliedVersions())
    }

    @Test
    fun `only missing migrations are applied on a partially migrated database`() {
        val migrator = DatabaseMigrator()
        migrator.migrateDatabase()
        val newest = migrator.knownVersions.max()
        transaction { SchemaTrackerTable.deleteWhere { version eq newest } }

        // If an older migration were re-applied, its tracker insert would violate the unique version index
        DatabaseMigrator().migrateDatabase()

        assertEquals(migrator.knownVersions, appliedVersions())
    }

    @Test
    fun `refuses to run when the database is newer than the latest known migration`() {
        val migrator = DatabaseMigrator()
        migrator.migrateDatabase()
        transaction {
            SchemaTrackerTable.insert {
                it[version] = 99
                it[date] = 0L
            }
        }

        val ex = assertFailsWith<IllegalStateException> { DatabaseMigrator().migrateDatabase() }
        assertTrue("refusing to start" in ex.message!!, "Unexpected message: ${ex.message}")
        assertEquals(migrator.knownVersions + 99, appliedVersions())
    }

    @Test
    fun `admin triggers from migration 02 protect the last admin`() {
        DatabaseMigrator().migrateDatabase()
        transaction {
            UsersTable.insert {
                it[username] = "admin"
                it[passwordHash] = "hash"
                it[admin] = true
            }
        }

        val demote = assertFailsWith<Exception> {
            transaction { UsersTable.update({ UsersTable.username eq "admin" }) { it[admin] = false } }
        }
        assertTrue("only admin" in demote.message!!, "Unexpected message: ${demote.message}")

        val delete = assertFailsWith<Exception> {
            transaction { UsersTable.deleteWhere { username eq "admin" } }
        }
        assertTrue("only admin" in delete.message!!, "Unexpected message: ${delete.message}")

        transaction {
            UsersTable.insert {
                it[username] = "admin2"
                it[passwordHash] = "hash"
                it[admin] = true
            }
            UsersTable.update({ UsersTable.username eq "admin" }) { it[admin] = false }
        }
        assertEquals(1, transaction { UsersTable.deleteWhere { username eq "admin" } })
    }
}
