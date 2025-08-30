package io.thoth.server.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.thoth.server.common.extensions.get
import io.thoth.server.common.utils.memoize
import io.thoth.server.config.DatabaseConnection
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.migrations.migrator.DatabaseMigrator
import mu.KotlinLogging.logger
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database

private object DatabaseFactory {
    private val log = logger {}
    private val dbInstance =
        { config: DatabaseConnection ->
            log.info { "Initialising database" }
            Database.connect(
                dataSource(config),
                databaseConfig = DatabaseConfig.invoke { useNestedTransactions = true },
            )
        }.memoize()

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
                    }.also { it.validate() }
            HikariDataSource(config)
        }.memoize()

    fun connect(config: DatabaseConnection) = dbInstance(config).run {}

    fun migrate(config: DatabaseConnection) {
        log.info("Migrating database")
        DatabaseMigrator(dbInstance(config)).updateDatabase()
        log.info("Migrations done")
    }
}

fun connectToDatabaseAndMigrate() {
    val config = get<ThothConfig>()
    DatabaseFactory.connect(config.database)
    DatabaseFactory.migrate(config.database)
}
