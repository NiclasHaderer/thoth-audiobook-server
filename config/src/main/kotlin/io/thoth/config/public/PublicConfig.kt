package io.thoth.config.public

import io.thoth.metadata.audible.models.AudibleRegions

interface PublicConfig {
    val ignoreFile: String
    val production: Boolean
    val domain: String
    val TLS: Boolean
    val audioFileLocations: List<String>
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

data class PublicConfigImpl(
    override val ignoreFile: String,
    override val production: Boolean,
    override val audioFileLocations: List<String>,
    override val analyzerThreads: Int,
    override val port: Int,
    override val domain: String,
    override val TLS: Boolean,
    override val audibleRegion: AudibleRegions,
    override val database: DatabaseConnectionImpl,
    override val configDirectory: String,
) : PublicConfig

data class DatabaseConnectionImpl(
    override val driverClassName: String,
    override val jdbcUrl: String,
    override val maximumPoolSize: Int,
    override val autoCommit: Boolean,
    override val transactionIsolation: String
) : DatabaseConnection
