package io.thoth.server.config

import com.cronutils.model.Cron
import io.thoth.metadata.audible.models.AudibleRegions

interface ThothConfig {
    val ignoreFile: String
    val production: Boolean
    val fullScanCron: Cron
    val metadataRefreshCron: Cron
    val domain: String
    val TLS: Boolean
    val analyzerThreads: Int
    val port: Int
    val audibleRegion: AudibleRegions
    val database: DatabaseConnection
    val configDirectory: String
}

interface DatabaseConnection {
    val driverClassName: String
    val jdbcUrl: String
    val maximumPoolSize: Int
    val autoCommit: Boolean
    val transactionIsolation: String
}

data class ThothConfigImpl(
    override val ignoreFile: String,
    override val production: Boolean,
    override val fullScanCron: Cron,
    override val metadataRefreshCron: Cron,
    override val analyzerThreads: Int,
    override val port: Int,
    override val domain: String,
    override val TLS: Boolean,
    override val audibleRegion: AudibleRegions,
    override val database: DatabaseConnectionImpl,
    override val configDirectory: String,
) : ThothConfig

data class DatabaseConnectionImpl(
    override val driverClassName: String,
    override val jdbcUrl: String,
    override val maximumPoolSize: Int,
    override val autoCommit: Boolean,
    override val transactionIsolation: String
) : DatabaseConnection
