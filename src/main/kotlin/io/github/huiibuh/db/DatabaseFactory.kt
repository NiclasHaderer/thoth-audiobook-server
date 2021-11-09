package io.github.huiibuh.db


import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.huiibuh.config.Settings
import io.github.huiibuh.db.migration.DatabaseMigrator
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory


object DatabaseFactory {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun connectAndMigrate() {
        connect()
        migrate()
    }

    fun connect() {
        log.info("Initialising database")
        val config = DatabaseConfig.invoke {
            useNestedTransactions = true
        }
        Database.connect(dataSource(), databaseConfig = config)
    }

    fun migrate() {
        log.info("Migrating database")
        val db = Database.connect(dataSource())
        DatabaseMigrator(db, "db.migration").runMigrations()
    }


    private fun dataSource(): HikariDataSource {
        val dbConfig = Settings.database
        val config = HikariConfig().apply {
            driverClassName = dbConfig.driverClassName
            jdbcUrl = dbConfig.jdbcUrl
            maximumPoolSize = dbConfig.maximumPoolSize
            isAutoCommit = dbConfig.autoCommit
            transactionIsolation = dbConfig.transactionIsolation
        }.also { it.validate() }
        return HikariDataSource(config)
    }


    suspend fun <T> dbQuery(
        block: suspend () -> T,
    ): T =
        newSuspendedTransaction { block() }
}
