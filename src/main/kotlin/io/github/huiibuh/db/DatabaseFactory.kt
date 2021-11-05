package io.github.huiibuh.db


import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.huiibuh.config.Settings
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import javax.sql.DataSource

object DatabaseFactory {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun connectAndMigrate() {
        connect()
        migrate()
    }

    fun connect() {
        log.info("Initialising database")
        val pool = hikari()
        Database.connect(pool)
    }

    fun migrate() {
        log.info("Migrating database")
        val pool = hikari()
        runFlyway(pool)
    }

    private fun hikari(): HikariDataSource {
        val dbConfig = Settings.Database
        val config = HikariConfig().apply {
            driverClassName = dbConfig.driverClassName
            jdbcUrl = dbConfig.jdbcUrl
            maximumPoolSize = dbConfig.maximumPoolSize
            isAutoCommit = dbConfig.autoCommit
            transactionIsolation = dbConfig.transactionValidation
            validate()
        }
        return HikariDataSource(config)
    }

    private fun runFlyway(datasource: DataSource) {
        val flyway = Flyway.configure()
                .dataSource(datasource)
                .load()
        try {
            flyway.info()
            flyway.migrate()
        } catch (e: Exception) {
            log.error("Exception running flyway migration", e)
            throw e
        }
        log.info("Flyway migration has finished")
    }

    suspend fun <T> dbQuery(
        block: suspend () -> T,
    ): T =
        newSuspendedTransaction { block() }
}
