package io.thoth.server.config

interface ThothConfig {
    val ignoreFile: String
    val production: Boolean
    val audioFileLocation: List<String>
    val analyzerThreads: Int
    val webUiPort: Int
    val audibleSearchHost: String
    val audibleAuthorHost: String
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
    override val audioFileLocation: List<String>,
    override val analyzerThreads: Int,
    override val webUiPort: Int,
    override val audibleSearchHost: String,
    override val audibleAuthorHost: String,
    override val database: DatabaseConnectionImpl,
    override val configDirectory: String
) : ThothConfig

data class DatabaseConnectionImpl(
    override val driverClassName: String,
    override val jdbcUrl: String,
    override val maximumPoolSize: Int,
    override val autoCommit: Boolean,
    override val transactionIsolation: String
) : DatabaseConnection
