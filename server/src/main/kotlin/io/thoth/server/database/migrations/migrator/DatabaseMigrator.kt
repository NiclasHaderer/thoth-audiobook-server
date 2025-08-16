package io.thoth.server.database.migrations.migrator

import io.thoth.server.common.extensions.tap
import mu.KotlinLogging.logger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import org.reflections.Reflections
import kotlin.reflect.full.createInstance

private data class MigrationHolder(
    val version: Int,
    val migration: Migration,
)

class DatabaseMigrator(
    private val db: Database,
    private val packageName: String = "io.thoth.server.database.migrations.history",
) {
    private val log = logger {}
    private val classNameMatcher = "(\\d+)_.*".toRegex()

    private val classes: List<Migration> by lazy {
        val reflections = Reflections(packageName)
        val classes = reflections.getSubTypesOf(Migration::class.java)
        try {
            classes.map { it.kotlin }.map { it.createInstance() }
        } catch (_: ClassCastException) {
            // Caused by hot reloading, so migrations should not be necessary, because the migration
            // has already run
            listOf()
        }
    }

    private val migrations: List<MigrationHolder> by lazy {
        classes
            .tap {
                if (!it::class.java.simpleName.contains(classNameMatcher)) {
                    throw IllegalStateException(
                        "Migration class name does not match the pattern: ${it::class.java.simpleName}",
                    )
                }
            }.filter { migration -> migration::class.java.simpleName.contains(classNameMatcher) }
            .map { migration ->
                val version = getVersionFromString(migration::class.java.simpleName)
                MigrationHolder(version, migration)
            }.sortedBy { it.version }
    }

    private fun getLatestVersion(): Int? =
        transaction(db = db) {
            SchemaTracker
                .all()
                .orderBy(TSchemaTrackers.version to SortOrder.DESC)
                .firstOrNull()
                ?.version
        }

    fun updateDatabase() {
        // Create table if not exist
        transaction(db = db) { SchemaUtils.create(TSchemaTrackers) }
        val latestVersion = getLatestVersion()
        executeUpdate(latestVersion)
    }

    private fun getVersionFromString(versionString: String): Int {
        // Cannot be null, because we filter after the regex
        val result = classNameMatcher.find(versionString)!!
        return result.groupValues[1].toInt()
    }

    private fun executeUpdate(latestDbVersion: Int?) {
        if (latestDbVersion == null) {
            log.info("No migrations found, applying all migrations")
            runMigrations(migrations)
            return
        }

        if (latestDbVersion > migrations.last().version) {
            log.info("Database version is higher than the latest migration version")
            log.info("Rolling back migrations to the latest migration version")
            runRollback(latestDbVersion)
            return
        }

        if (latestDbVersion == migrations.last().version) {
            log.info("Database is up to date")
            return
        }
    }

    private fun runRollback(latestDbVersion: Int) {
        SchemaTracker
            .find { TSchemaTrackers.version greater latestDbVersion }
            .orderBy(TSchemaTrackers.version to SortOrder.DESC)
            .forEach {
                log.info("Rolling back migration ${it.version}")
                transaction(db = db) {
                    try {
                        it.rollback.split(Char.MIN_VALUE).forEach { statement -> exec(statement) }
                    } catch (e: Exception) {
                        rollback()
                        log.error(e) { "Error while rolling back migration ${it.version}" }
                        throw e
                    }
                }
                it.delete()
            }
    }

    private fun runMigrations(migrations: List<MigrationHolder>) {
        migrations.forEach {
            try {
                transaction(db = db) {
                    log.info("Applying migration ${it.migration.javaClass.simpleName}")
                    it.migration.migrate(db)
                    saveMigrationInDatabase(it)
                }
            } catch (e: Exception) {
                log.error("Error while applying migration ${it.migration.javaClass.simpleName}", e)
                throw e
            }
        }
    }

    private fun saveMigrationInDatabase(migrationHolder: MigrationHolder) =
        transaction(db = db) {
            SchemaTracker.new {
                date = System.currentTimeMillis() / 1000L
                version = migrationHolder.version
                rollback =
                    migrationHolder.migration
                        .generateRollbackStatements(db)
                        .joinToString(separator = Char.MIN_VALUE.toString())
            }
        }
}
