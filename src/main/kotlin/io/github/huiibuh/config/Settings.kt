package io.github.huiibuh.config

interface DatabaseConnection {
    val driverClassName: String
    val jdbcUrl: String
    val maximumPoolSize: Int
    val autoCommit: Boolean
    val transactionIsolation: String
}


object Settings {
    val ignoreFile: String by lazy {
        System.getenv("IGNORE_FILE") ?: ".audignore"
    }
    val production: Boolean by lazy {
        System.getenv("PRODUCTION")?.toBooleanStrictOrNull() ?: false
    }

    val audioFileLocation: String by lazy {
        System.getenv("AUDIO_FILE_LOCATION") ?: "${System.getProperty("user.home")}/Music/books"
    }

    val webUiPort: Int by lazy {
        System.getenv("WEB_UI_PORT")?.toIntOrNull() ?: 8080
    }

    val database: DatabaseConnection
        get() = if (production) SqLite else SqLite

    private object H2Database : DatabaseConnection {
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

    private object SqLite : DatabaseConnection {
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
}
