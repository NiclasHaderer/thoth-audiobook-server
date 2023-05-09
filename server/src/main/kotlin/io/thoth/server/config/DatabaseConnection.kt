package io.thoth.server.config

object SqLite : DatabaseConnection {
    override val driverClassName: String by lazy { System.getenv("DB_DRIVER_CLASS_NAME") ?: "org.sqlite.JDBC" }
    val sqlitePath: String by lazy { System.getenv("SQLITE_PATH") ?: "audiobook.db" }
    override val jdbcUrl: String by lazy { System.getenv("DB_JDBC_URL") ?: "jdbc:sqlite:$sqlitePath" }
    override val maximumPoolSize = 1
    override val autoCommit: Boolean by lazy { System.getenv("DB_AUTO_COMMIT")?.toBooleanStrictOrNull() ?: false }
    override val transactionIsolation: String by lazy {
        System.getenv("DB_TRANSACTION_VALIDATION") ?: "TRANSACTION_SERIALIZABLE"
    }
}
