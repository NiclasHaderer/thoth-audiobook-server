package io.github.huiibuh.db


import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.huiibuh.db.migrator.DatabaseMigrator
import io.github.huiibuh.extensions.classLogger
import io.github.huiibuh.settings.Settings
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


object DatabaseFactory : KoinComponent {
    private val log = classLogger()
    private val settings by inject<Settings>()
    private val dbInstance by lazy {
        log.info("Initialising database")
        Database.connect(dataSource, databaseConfig = DatabaseConfig.invoke {
            useNestedTransactions = true
        })
    }

    private val dataSource by lazy {
        val dbConfig = settings.database
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
        DatabaseMigrator(dbInstance, "io.github.huiibuh.db.migrations").runMigrations()
        log.info("Migrations done")
    }

}
