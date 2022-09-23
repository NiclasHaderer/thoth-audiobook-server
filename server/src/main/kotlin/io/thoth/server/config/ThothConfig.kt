package io.thoth.server.config

import java.security.KeyPair

interface ThothConfig {
    val ignoreFile: String
    val production: Boolean
    val audioFileLocation: String
    val analyzerThreads: Int
    val webUiPort: Int
    val audibleSearchHost: String
    val audibleAuthorHost: String
    val database: DatabaseConnection
    val keyPair: KeyPair
}

interface DatabaseConnection {
    val driverClassName: String
    val jdbcUrl: String
    val maximumPoolSize: Int
    val autoCommit: Boolean
    val transactionIsolation: String
}
