package io.thoth.server.config

import io.thoth.metadata.audible.client.AudibleRegions

interface ThothConfig {
    val ignoreFile: String
    val production: Boolean
    val audioFileLocations: List<String>
    val analyzerThreads: Int
    val port: Int
    val audibleRegion: AudibleRegions
    val database: DatabaseConnection
    val configDirectory: String
    val preferEmbeddedMetadata: Boolean
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
    override val audioFileLocations: List<String>,
    override val analyzerThreads: Int,
    override val port: Int,
    override val audibleRegion: AudibleRegions,
    override val database: DatabaseConnectionImpl,
    override val configDirectory: String,
    override val preferEmbeddedMetadata: Boolean
) : ThothConfig

data class DatabaseConnectionImpl(
    override val driverClassName: String,
    override val jdbcUrl: String,
    override val maximumPoolSize: Int,
    override val autoCommit: Boolean,
    override val transactionIsolation: String
) : DatabaseConnection
