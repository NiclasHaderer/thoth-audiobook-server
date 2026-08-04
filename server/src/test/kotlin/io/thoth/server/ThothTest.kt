package io.thoth.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariDataSource
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.DatabaseConnector
import io.thoth.server.database.migrations.DatabaseMigrator
import io.thoth.server.database.sqliteDataSource
import io.thoth.server.di.serialization.JacksonSerialization
import io.thoth.server.di.thothModule
import io.thoth.server.file.scanner.LibraryImportPipeline
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform.getKoin
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

abstract class ThothTest(
    private val migrate: Boolean = true,
) {
    protected lateinit var dataDir: Path
        private set

    protected lateinit var config: ThothConfig
        private set

    protected lateinit var database: Database
        private set

    private lateinit var dataSource: HikariDataSource

    protected open fun configure(dataDir: Path) = ThothConfig(dataDir = dataDir)

    @BeforeTest
    fun startApplication() {
        dataDir = createTempDirectory("thoth-test")
        config = configure(dataDir)
        startKoin { modules(thothModule(config)) }
        // Ktor normally supplies the mapper through configureSerialization, which no test installs.
        getKoin().get<JacksonSerialization>().objectMapper = jacksonObjectMapper()

        dataSource = sqliteDataSource(config.sqliteFile, config.importThreads, busyTimeoutMillis = 500)
        database = DatabaseConnector.connect(dataSource)
        if (migrate) DatabaseMigrator().migrateDatabase()
        getKoin().get<LibraryImportPipeline>().start()
    }

    @AfterTest
    fun stopApplication() {
        getKoin().get<LibraryImportPipeline>().stop()
        TransactionManager.closeAndUnregister(database)
        dataSource.close()
        stopKoin()
        dataDir.toFile().deleteRecursively()
    }
}
