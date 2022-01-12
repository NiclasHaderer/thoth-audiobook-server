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
    override val maximumPoolSize = 3
    override val autoCommit = false
    override val transactionIsolation = "TRANSACTION_REPEATABLE_READ"
}

internal object SqLite : DatabaseConnection {
    private const val sqlitePath = "audiobook.db"
    override val driverClassName = "org.sqlite.JDBC"
    override val jdbcUrl = "jdbc:sqlite:${this.sqlitePath}"
    override val maximumPoolSize = 1
    override val autoCommit = false
    override val transactionIsolation = "TRANSACTION_SERIALIZABLE"
}
