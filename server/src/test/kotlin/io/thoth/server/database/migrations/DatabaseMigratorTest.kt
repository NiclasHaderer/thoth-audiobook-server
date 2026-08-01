package io.thoth.server.database.migrations

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.thoth.server.database.tables.UsersTable
import io.thoth.server.di.serialization.JacksonSerialization
import io.thoth.server.di.serialization.Serialization
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DatabaseMigratorTest {
    private lateinit var dbFile: File

    @BeforeTest
    fun setup() {
        // LibrariesTable's json columns resolve their serializer through Koin while the table object initializes,
        // so Koin has to be up before any table is touched.
        startKoin {
            modules(
                module {
                    single { JacksonSerialization().apply { objectMapper = jacksonObjectMapper() } }
                    single<Serialization> { get<JacksonSerialization>() }
                },
            )
        }
        dbFile = File.createTempFile("thoth-migrator-test", ".db")
        Database.connect("jdbc:sqlite:${dbFile.absolutePath}", "org.sqlite.JDBC")
    }

    @AfterTest
    fun teardown() {
        stopKoin()
        dbFile.delete()
    }

    private fun appliedVersions(): List<Int> = transaction { SchemaTrackerEntity.all().map { it.version }.sorted() }

    private fun tableNames(): List<String> =
        transaction {
            exec("SELECT name FROM sqlite_master WHERE type='table'") { rs ->
                buildList { while (rs.next()) add(rs.getString(1)) }
            }!!
        }

    @Test
    fun `fresh database gets all migrations applied`() {
        DatabaseMigrator().migrateDatabase()

        assertEquals(listOf(1, 2), appliedVersions())
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
        DatabaseMigrator().migrateDatabase()
        DatabaseMigrator().migrateDatabase()

        assertEquals(listOf(1, 2), appliedVersions())
    }

    @Test
    fun `only missing migrations are applied on a partially migrated database`() {
        DatabaseMigrator().migrateDatabase()
        transaction { SchemaTrackerTable.deleteWhere { version eq 2 } }

        // If migration 1 were re-applied, its tracker insert would violate the unique version index
        DatabaseMigrator().migrateDatabase()

        assertEquals(listOf(1, 2), appliedVersions())
    }

    @Test
    fun `refuses to run when the database is newer than the latest known migration`() {
        DatabaseMigrator().migrateDatabase()
        transaction {
            SchemaTrackerTable.insert {
                it[version] = 99
                it[date] = 0L
            }
        }

        val ex = assertFailsWith<IllegalStateException> { DatabaseMigrator().migrateDatabase() }
        assertTrue("refusing to start" in ex.message!!, "Unexpected message: ${ex.message}")
        assertEquals(listOf(1, 2, 99), appliedVersions())
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
