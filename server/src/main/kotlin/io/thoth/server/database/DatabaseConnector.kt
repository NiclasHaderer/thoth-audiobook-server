package io.thoth.server.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.thoth.server.config.DatabaseType
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.migrations.DatabaseMigrator
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.SQLiteDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Path
import kotlin.io.path.absolutePathString

fun sqliteUrl(databaseFile: Path): String =
    "jdbc:sqlite:${databaseFile.absolutePathString()}" +
        "?journal_mode=WAL" +
        "&synchronous=NORMAL" +
        "&journal_size_limit=${64 * 1024 * 1024}" +
        "&busy_timeout=5000" +
        "&foreign_keys=ON" +
        // Negative means KiB, and it is per connection
        "&cache_size=-8000"

fun sqliteDataSource(databaseFile: Path): HikariDataSource =
    hikariDataSource {
        driverClassName = "org.sqlite.JDBC"
        jdbcUrl = sqliteUrl(databaseFile)
        maximumPoolSize = 4
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
        val dbConfig = config.database
        val dataSource =
            when (dbConfig.type) {
                DatabaseType.SQLITE -> {
                    sqliteDataSource(config.sqliteFile)
                }

                DatabaseType.POSTGRES -> {
                    hikariDataSource {
                        driverClassName = "org.postgresql.Driver"
                        jdbcUrl = "jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/${dbConfig.name}"
                        username = dbConfig.user
                        password = dbConfig.password
                        maximumPoolSize = 10
                    }
                }
            }

        dbInstance = connect(dataSource)

        log.info { "Migrating database" }
        DatabaseMigrator().migrateDatabase()
        log.info { "Migrations done" }
    }

    fun connect(dataSource: javax.sql.DataSource): Database {
        val database =
            Database.connect(dataSource, databaseConfig = DatabaseConfig.invoke { useNestedTransactions = true })

        transaction(database) {
            val dialect = currentDialect
            require(dialect is SQLiteDialect || dialect is PostgreSQLDialect) {
                "Unsupported database dialect '${dialect.name}'. Thoth supports only SQLite and PostgreSQL."
            }
        }
        return database
    }
}
