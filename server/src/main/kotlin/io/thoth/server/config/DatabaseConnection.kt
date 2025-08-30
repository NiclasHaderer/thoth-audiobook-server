package io.thoth.server.config

data class DatabaseConnection(
    val driverClassName: String,
    val jdbcUrl: String,
    val maximumPoolSize: Int,
    val autoCommit: Boolean,
    val transactionIsolation: String,
)
