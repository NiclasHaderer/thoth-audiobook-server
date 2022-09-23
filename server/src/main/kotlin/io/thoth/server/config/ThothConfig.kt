package io.thoth.server.config

interface KeyPairLocation {
    val publicKeyLocation: String
    val privateKeyLocation: String
}

interface ThothConfig {
    val ignoreFile: String
    val production: Boolean
    val audioFileLocation: String
    val analyzerThreads: Int
    val webUiPort: Int
    val audibleSearchHost: String
    val audibleAuthorHost: String
    val database: DatabaseConnection
    val keyPair: KeyPairLocation
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
    override val audioFileLocation: String,
    override val analyzerThreads: Int,
    override val webUiPort: Int,
    override val audibleSearchHost: String,
    override val audibleAuthorHost: String,
    override val database: DatabaseConnectionImpl,
    override val keyPair: KeyPairLocationImpl
) : ThothConfig

data class DatabaseConnectionImpl(
    override val driverClassName: String,
    override val jdbcUrl: String,
    override val maximumPoolSize: Int,
    override val autoCommit: Boolean,
    override val transactionIsolation: String
) : DatabaseConnection

data class KeyPairLocationImpl(
    override val publicKeyLocation: String, override val privateKeyLocation: String
) : KeyPairLocation
