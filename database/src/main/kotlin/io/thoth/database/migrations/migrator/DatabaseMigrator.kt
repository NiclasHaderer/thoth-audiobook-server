package io.thoth.database.migrations.migrator

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import kotlin.reflect.full.createInstance

class DatabaseMigrator(private val db: Database, private val packageName: String = "io.thoth.database.migrations.history") {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val classNameMatcher = "(\\d+)_.*".toRegex()

    private val classes: List<Migration> by lazy {
        val reflections = Reflections(packageName)
        val classes = reflections.getSubTypesOf(Migration::class.java)
        try {
            classes.map { it.kotlin }.map { it.createInstance() }
        } catch (_: ClassCastException) {
            // Caused hot reloading, so migrations should not be necessary, because the migration has already run
            listOf(null)
            listOf()
        }
    }

    private val sortedMigrations: List<Migration> by lazy {
        classes
            .filter { migration -> migration::class.java.simpleName.contains(classNameMatcher) }
            .sortedBy { migration -> migration::class.java.simpleName }
    }

    fun runMigrations() {
        // Create table if not exist
        transaction(db = db) {
            SchemaUtils.create(TSchemaTrackers)
        }

        // Get all applied migrations
        val appliedMigrations = transaction(db = db) {
            SchemaTracker.all()
        }
        // Get the latest version of applied migration versions
        val latestVersion = try {
            transaction(db = db) { appliedMigrations.maxOf { it.version } }
        } catch (_: Exception) {
            -1
        }
        applyMigrations(latestVersion)
    }

    private fun getVersionFromString(versionString: String): Int {
        // Cannot be null, because we filter after the regex
        val result = classNameMatcher.find(versionString)!!
        return result.groupValues[1].toInt()
    }

    private fun applyMigrations(fromVersion: Int) {
        sortedMigrations.forEach {
            try {
                val migrationVersion = getVersionFromString(it.javaClass.simpleName)
                if (migrationVersion > fromVersion) {
                    log.info("Applying migration ${it.javaClass.simpleName}")
                    it.migrate(db)
                    saveMigrationInDatabase(migrationVersion)
                }
            } catch (migrationException: Exception) {
                log.error("Error during migration")
                log.error("Applying rollback")
                val rollbackException = try {
                    it.rollback(db)
                    null
                } catch (rollbackException: Exception) {
                    rollbackException
                }

                log.error(migrationException.message, migrationException)
                if (rollbackException != null) {
                    log.error(rollbackException.message, rollbackException)
                } else {
                    log.error("Rollback succeeded")
                }
                throw Exception("Could not migrate.")
            }
        }
    }

    private fun saveMigrationInDatabase(migrationVersion: Int) = transaction(db = db) {
        SchemaTracker.new {
            date = System.currentTimeMillis() / 1000L
            version = migrationVersion
        }
    }

}
