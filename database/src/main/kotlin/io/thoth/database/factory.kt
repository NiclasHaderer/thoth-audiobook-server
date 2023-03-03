package io.thoth.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.thoth.common.utils.memoize
import io.thoth.config.DatabaseConnection
import io.thoth.database.migrations.migrator.DatabaseMigrator
import mu.KotlinLogging.logger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig

private object DatabaseFactory {
    private val log = logger {}
    private val dbInstance =
        { config: DatabaseConnection ->
                log.info { "Initialising database" }
                Database.connect(
                    dataSource(config),
                    databaseConfig = DatabaseConfig.invoke { useNestedTransactions = true }
                )
            }
            .memoize()

    private val dataSource =
        { dbConfig: DatabaseConnection ->
                val config =
                    HikariConfig()
                        .apply {
                            driverClassName = dbConfig.driverClassName
                            jdbcUrl = dbConfig.jdbcUrl
                            maximumPoolSize = dbConfig.maximumPoolSize
                            isAutoCommit = dbConfig.autoCommit
                            transactionIsolation = dbConfig.transactionIsolation
                        }
                        .also { it.validate() }
                HikariDataSource(config)
            }
            .memoize()

    fun connect(config: DatabaseConnection) = dbInstance(config).run {}

    fun migrate(config: DatabaseConnection) {
        log.info("Migrating database")
        DatabaseMigrator(dbInstance(config)).updateDatabase()
        log.info("Migrations done")
    }
}

fun connectToDatabase(config: DatabaseConnection) {
    DatabaseFactory.connect(config)
}

fun migrateDatabase(config: DatabaseConnection) {
    DatabaseFactory.migrate(config)
}
