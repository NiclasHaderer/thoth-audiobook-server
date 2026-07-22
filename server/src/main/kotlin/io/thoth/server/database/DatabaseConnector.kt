package io.thoth.server.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
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

object DatabaseConnector : KoinComponent {
    private val log = logger {}
    val config by inject<ThothConfig>()

    private lateinit var dbInstance: Database

    fun connect() {
        val dbConfig = config.database
        val dataSource =
            HikariConfig()
                .apply {
                    driverClassName = dbConfig.driverClassName
                    jdbcUrl = dbConfig.jdbcUrl
                    maximumPoolSize = dbConfig.maximumPoolSize
                    isAutoCommit = dbConfig.autoCommit
                    transactionIsolation = dbConfig.transactionIsolation
                }.also { it.validate() }
                .let { HikariDataSource(it) }

        dbInstance =
            Database.connect(
                dataSource,
                databaseConfig = DatabaseConfig.invoke { useNestedTransactions = true },
            )

        transaction(dbInstance) {
            val dialect = currentDialect
            require(dialect is SQLiteDialect || dialect is PostgreSQLDialect) {
                "Unsupported database dialect '${dialect.name}'. Thoth supports only SQLite and PostgreSQL."
            }
        }

        log.info { "Migrating database" }
        DatabaseMigrator().migrateDatabase()
        log.info { "Migrations done" }
    }
}
