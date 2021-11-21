package io.github.huiibuh.db.migration

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import kotlin.reflect.full.createInstance

class DatabaseMigrator(val db: Database, val packageName: String) {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val classNameMatcher = "(d+)_.*".toRegex()

    private val classes: List<Migration> by lazy {
        val reflections = Reflections(this.packageName)
        val classes = reflections.getSubTypesOf(Migration::class.java)
        classes.map { it.kotlin }.map { it.createInstance() }
    }

    private val sortedMigrations: List<Migration> by lazy {
        classes
                .filter { migration -> migration::class.java.simpleName.contains(classNameMatcher) }
                .sortedBy { migration -> migration::class.java.simpleName }
    }

    fun runMigrations() {
        // Create table if not exist
        transaction {
            SchemaUtils.create(SchemaTrackers)
        }

        // Get all applied migrations
        val appliedMigrations = transaction {
            SchemaTracker.all()
        }
        // Get the latest version of applied migration versions
        val latestVersion = appliedMigrations.maxOf { it.version }
        applyMigrations(latestVersion)
    }

    private fun getVersionFromString(versionString: String): Int {
        // Cannot be null, because we filter after the regex
        val result = classNameMatcher.find(versionString)!!
        return result.groupValues[0].toInt()
    }

    private fun applyMigrations(fromVersion: Int) {
        sortedMigrations.forEach {
            try {
                val migrationVersion = getVersionFromString(it.javaClass.simpleName)
                if (migrationVersion > fromVersion) {
                    it.migrate(db)
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

                log.error(migrationException.toString())
                if (rollbackException != null) {
                    log.error(rollbackException.toString())
                } else {
                    log.error("Rollback succeeded")
                }
                throw Exception("Could not migrate.")
            }
        }
    }

}
