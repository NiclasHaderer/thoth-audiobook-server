package io.github.huiibuh.settings

interface DatabaseConnection {
    val driverClassName: String
    val jdbcUrl: String
    val maximumPoolSize: Int
    val autoCommit: Boolean
    val transactionIsolation: String
}


internal  object H2Database : DatabaseConnection {
    override val driverClassName: String by lazy {
        System.getenv("DB_DRIVER_CLASS_NAME") ?: "org.h2.Driver"
    }
    override val jdbcUrl: String by lazy {
        System.getenv("DB_JDBC_URL") ?: "jdbc:h2:mem:test"
    }
    override val maximumPoolSize: Int by lazy {
        System.getenv("DB_POOL_SIZE")?.toIntOrNull() ?: 3
    }
    override val autoCommit: Boolean by lazy {
        System.getenv("DB_AUTO_COMMIT")?.toBooleanStrictOrNull() ?: false
    }
    override val transactionIsolation: String by lazy {
        System.getenv("DB_TRANSACTION_VALIDATION") ?: "TRANSACTION_REPEATABLE_READ"
    }
}

internal object SqLite : DatabaseConnection {
    override val driverClassName: String by lazy {
        System.getenv("DB_DRIVER_CLASS_NAME") ?: "org.sqlite.JDBC"
    }
    val sqlitePath: String by lazy {
        System.getenv("SQLITE_PATH") ?: "audiobook.db"
    }
    override val jdbcUrl: String by lazy {
        System.getenv("DB_JDBC_URL") ?: "jdbc:sqlite:${this.sqlitePath}"
    }
    override val maximumPoolSize = 1
    override val autoCommit: Boolean by lazy {
        System.getenv("DB_AUTO_COMMIT")?.toBooleanStrictOrNull() ?: false
    }
    override val transactionIsolation: String by lazy {
        System.getenv("DB_TRANSACTION_VALIDATION") ?: "TRANSACTION_SERIALIZABLE"
    }
}
