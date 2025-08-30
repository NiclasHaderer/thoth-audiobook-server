package io.thoth.server.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.migrations.DatabaseMigrator
import mu.KotlinLogging.logger
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
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
        log.info("Migrating database")
        DatabaseMigrator().migrateDatabase()
        log.info("Migrations done")
    }
}
