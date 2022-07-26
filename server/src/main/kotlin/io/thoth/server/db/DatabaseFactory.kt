package io.thoth.server.db


import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.thoth.database.migrations.migrator.DatabaseMigrator
import io.thoth.server.config.ThothConfig
import mu.KotlinLogging.logger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


private object DatabaseFactory : KoinComponent {
    private val log = logger {}
    private val thothConfig by inject<ThothConfig>()
    private val dbInstance by lazy {
        log.info { "Initialising database" }
        Database.connect(dataSource, databaseConfig = DatabaseConfig.invoke {
            useNestedTransactions = true
        })
    }

    private val dataSource by lazy {
        val dbConfig = thothConfig.database
        val config = HikariConfig().apply {
            driverClassName = dbConfig.driverClassName
            jdbcUrl = dbConfig.jdbcUrl
            maximumPoolSize = dbConfig.maximumPoolSize
            isAutoCommit = dbConfig.autoCommit
            transactionIsolation = dbConfig.transactionIsolation
        }.also { it.validate() }
        HikariDataSource(config)
    }

    fun connect() = dbInstance.run { }

    fun migrate() {
        log.info("Migrating database")
        DatabaseMigrator(dbInstance).runMigrations()
        log.info("Migrations done")
    }

}

fun connectToDatabase() {
    DatabaseFactory.connect()
}

fun migrateDatabase() {
    DatabaseFactory.migrate()
}
