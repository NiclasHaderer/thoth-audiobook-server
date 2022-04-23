package io.github.huiibuh.settings

interface DatabaseConnection {
    val driverClassName: String
    val jdbcUrl: String
    val maximumPoolSize: Int
    val autoCommit: Boolean
    val transactionIsolation: String
}


internal object H2Database : DatabaseConnection {
    override val driverClassName = "org.h2.Driver"
    override val jdbcUrl = "jdbc:h2:mem:regular"
    override val maximumPoolSize = 100
    override val autoCommit = false
    override val transactionIsolation = "TRANSACTION_REPEATABLE_READ"
}

internal object ProdDatabaseConnection : DatabaseConnection {
    override val driverClassName: String by lazy {
        System.getenv("DB_DRIVER_CLASS_NAME")
    }
    val sqlitePath: String by lazy {
        System.getenv("SQLITE_PATH")
    }
    override val jdbcUrl: String by lazy {
        System.getenv("DB_JDBC_URL")
    }
    override val maximumPoolSize = 1
    override val autoCommit: Boolean by lazy {
        System.getenv("DB_AUTO_COMMIT").toBooleanStrict()
    }
    override val transactionIsolation: String by lazy {
        System.getenv("DB_TRANSACTION_VALIDATION")
    }
}
