package io.thoth.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.DatabaseConnector
import io.thoth.server.database.migrations.DatabaseMigrator
import io.thoth.server.database.sqliteDataSource
import io.thoth.server.di.serialization.JacksonSerialization
import io.thoth.server.di.thothModule
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform.getKoin
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.toPath
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

    @BeforeTest
    fun startApplication() {
        dataDir = createTempDirectory("thoth-test")
        config = ThothConfig(dataDir = dataDir)
        startKoin { modules(thothModule(config)) }
        // Ktor normally supplies the mapper through configureSerialization, which no test installs.
        getKoin().get<JacksonSerialization>().objectMapper = jacksonObjectMapper()

        database = DatabaseConnector.connect(sqliteDataSource(config.sqliteFile))
        if (migrate) DatabaseMigrator().migrateDatabase()
    }

    @AfterTest
    fun stopApplication() {
        stopKoin()
        dataDir.toFile().deleteRecursively()
    }
}
