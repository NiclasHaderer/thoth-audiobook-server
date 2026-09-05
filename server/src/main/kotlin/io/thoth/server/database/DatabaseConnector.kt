package io.thoth.server.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.migrations.DatabaseMigrator
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.vendors.SQLiteDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Path
import kotlin.io.path.absolutePathString

const val SQLITE_BUSY_TIMEOUT_MILLIS = 5000

fun sqliteUrl(
    databaseFile: Path,
    busyTimeoutMillis: Int = SQLITE_BUSY_TIMEOUT_MILLIS,
): String =
    "jdbc:sqlite:${databaseFile.absolutePathString()}" +
        "?journal_mode=WAL" +
        "&synchronous=NORMAL" +
        "&journal_size_limit=${64 * 1024 * 1024}" +
        "&busy_timeout=$busyTimeoutMillis" +
        "&foreign_keys=ON" +
        // Negative means KiB, and it is per connection
        "&cache_size=-8000"

fun sqliteDataSource(
    databaseFile: Path,
    importThreads: Int = 4,
    busyTimeoutMillis: Int = SQLITE_BUSY_TIMEOUT_MILLIS,
): HikariDataSource =
    hikariDataSource {
        driverClassName = "org.sqlite.JDBC"
        jdbcUrl = sqliteUrl(databaseFile, busyTimeoutMillis)
        // WAL readers do not conflict with the writer, but the import workers all read concurrently and
        // would otherwise starve the Ktor handlers out of the pool.
        maximumPoolSize = 4 + importThreads
        transactionIsolation = "TRANSACTION_SERIALIZABLE"
    }

private fun hikariDataSource(configure: HikariConfig.() -> Unit): HikariDataSource =
    HikariConfig()
        .apply {
            configure()
            isAutoCommit = false
        }.also { it.validate() }
        .let { HikariDataSource(it) }

object DatabaseConnector : KoinComponent {
    private val log = logger {}
    val config by inject<ThothConfig>()

    private lateinit var dbInstance: Database

    fun connect() {
        dbInstance = connect(sqliteDataSource(config.sqliteFile, config.importThreads))

        log.info { "Migrating database" }
        DatabaseMigrator().migrateDatabase()
        log.info { "Migrations done" }
    }

    fun connect(dataSource: javax.sql.DataSource): Database {
        val database =
            Database.connect(
                dataSource,
                databaseConfig =
                    DatabaseConfig.invoke {
                        useNestedTransactions = true
                        // A write that loses a race against another writer comes back as SQLITE_BUSY at once
                        // instead of going through busy_timeout, and Exposed retries with no delay by default
                        defaultMinRetryDelay = 50
                        defaultMaxRetryDelay = 500
                    },
            )

        transaction(database) {
            val dialect = currentDialect
            require(dialect is SQLiteDialect) {
                "Unsupported database dialect '${dialect.name}'. Thoth runs on SQLite."
            }
        }
        return database
    }
}
