package io.github.huiibuh.config


object Settings {
    val production: Boolean by lazy {
        System.getenv("PRODUCTION").toBooleanStrictOrNull() ?: false
    }

    object Database {
        val driverClassName: String by lazy {
            System.getenv("DB_DRIVER_CLASS_NAME") ?: "org.h2.Driver"
        }
        val jdbcUrl: String by lazy {
            System.getenv("DB_JDBC_URL") ?: "jdbc:h2:mem:test"
        }
        val maximumPoolSize: Int by lazy {
            System.getenv("DB_POOL_SIZE").toIntOrNull() ?: 3
        }
        val autoCommit: Boolean by lazy {
            System.getenv("DB_AUTO_COMMIT").toBooleanStrictOrNull() ?: false
        }
        val transactionValidation: String by lazy {
            System.getenv("DB_TRANSACTION_VALIDATION") ?: "TRANSACTION_REPEATABLE_READ"
        }
    }
}
