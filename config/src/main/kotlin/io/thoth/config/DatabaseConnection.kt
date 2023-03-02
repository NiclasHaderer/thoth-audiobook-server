package io.thoth.config

internal object H2Database : DatabaseConnection {
  override val driverClassName = "org.h2.Driver"
  override val jdbcUrl = "jdbc:h2:file:~/db\\\\.;IFEXISTS=TRUE"
  override val maximumPoolSize = 100
  override val autoCommit = false
  override val transactionIsolation = "TRANSACTION_REPEATABLE_READ"
}

internal object SqLite : DatabaseConnection {
  override val driverClassName: String by lazy {
    System.getenv("DB_DRIVER_CLASS_NAME") ?: "org.sqlite.JDBC"
  }
  val sqlitePath: String by lazy { System.getenv("SQLITE_PATH") ?: "audiobook.db" }
  override val jdbcUrl: String by lazy { System.getenv("DB_JDBC_URL") ?: "jdbc:sqlite:$sqlitePath" }
  override val maximumPoolSize = 1
  override val autoCommit: Boolean by lazy {
    System.getenv("DB_AUTO_COMMIT")?.toBooleanStrictOrNull() ?: false
  }
  override val transactionIsolation: String by lazy {
    System.getenv("DB_TRANSACTION_VALIDATION") ?: "TRANSACTION_SERIALIZABLE"
  }
}

internal object ProdDatabaseConnection : DatabaseConnection {
  override val driverClassName: String by lazy { System.getenv("DB_DRIVER_CLASS_NAME") }
  val sqlitePath: String by lazy { System.getenv("SQLITE_PATH") }
  override val jdbcUrl: String by lazy { System.getenv("DB_JDBC_URL") }
  override val maximumPoolSize = 1
  override val autoCommit: Boolean by lazy { System.getenv("DB_AUTO_COMMIT").toBooleanStrict() }
  override val transactionIsolation: String by lazy { System.getenv("DB_TRANSACTION_VALIDATION") }
}
