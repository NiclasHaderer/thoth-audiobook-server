package io.github.huiibuh.db


import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.huiibuh.settings.Settings
import io.github.huiibuh.db.migration.DatabaseMigrator
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory


object DatabaseFactory : KoinComponent {
    private val settings by inject<Settings>()
    private var dbInstance: Database? = null
    private val log = LoggerFactory.getLogger(this::class.java)
    private val dbConfig = DatabaseConfig.invoke {
        useNestedTransactions = true
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

    fun connect() {
        log.info("Initialising database")
        dbInstance = Database.connect(dataSource, databaseConfig = dbConfig)
    }

    fun migrate() {
        log.info("Migrating database")
        if (dbInstance == null) {
            throw Exception("Please connect to the database before running the migrations")
        }

        DatabaseMigrator(dbInstance!!, "db.migrations").runMigrations()
        log.info("Migrations done")
    }

}
