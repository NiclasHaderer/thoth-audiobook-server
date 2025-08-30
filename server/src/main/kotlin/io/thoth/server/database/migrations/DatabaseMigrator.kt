package io.thoth.server.database.migrations

import mu.KotlinLogging.logger
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.reflections.Reflections

private data class DatabaseVersion(
    val version: Int,
    val name: String,
    private val migration: Migration,
) {
    private val log = logger {}

    override fun toString(): String = "Version $version '$name'"

    fun migrate() {
        try {
            transaction {
                log.info("Applying migration ${this@DatabaseVersion}")
                migration.migrate()
                SchemaTrackerEntity.new {
                    date = System.currentTimeMillis() / 1000L
                    version = this@DatabaseVersion.version
                }
            }
        } catch (e: Exception) {
            log.error("Error while applying migration ${this@DatabaseVersion}", e)
            throw e
        }
    }
}

class DatabaseMigrator {
    private val log = logger {}
    private val classNameMatcher = "(\\d+)_(.*)".toRegex()
    private val packageName: String = "io.thoth.server.database.migrations.history"

    private val databaseVersions: List<DatabaseVersion> by lazy {
        Reflections(packageName)
            .getSubTypesOf(Migration::class.java)
            .map {
                val versionMatch =
                    classNameMatcher.find(it.simpleName) ?: run {
                        log.error { "Class ${it.name} does not match migration pattern" }
                        return@map null
                    }
                val version = versionMatch.groupValues[1].toInt()
                val name = versionMatch.groupValues[2]
                DatabaseVersion(version, name, it.getDeclaredConstructor().newInstance())
            }.filterNotNull()
            .sortedBy { it.version }
    }

    private val latestAppliedVersion by lazy {
        transaction {
            SchemaTrackerEntity
                .all()
                .orderBy(SchemaTrackerTable.version to SortOrder.DESC)
                .firstOrNull()
                ?.version ?: -1
        }
    }

    fun migrateDatabase() {
        transaction { SchemaUtils.create(SchemaTrackerTable) }
        migrateTo(latestAppliedVersion)
    }

    private fun migrateTo(latestDbVersion: Int) {
        if (latestDbVersion > databaseVersions.last().version) {
            log.error("Database version is higher than the latest migration version")
            throw Exception("Your thoth version is older, than the newest database. Downgrading not supported")
        }

        if (latestDbVersion == databaseVersions.last().version) {
            log.info("Database is up to date")
            return
        }

        databaseVersions.forEach {
            if (it.version <= latestDbVersion) {
                log.info { "Skipping $it, because it was already applied" }
            } else {
                it.migrate()
            }
        }
    }
}
