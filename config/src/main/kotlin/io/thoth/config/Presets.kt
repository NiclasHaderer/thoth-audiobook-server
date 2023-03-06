package io.thoth.config

import io.thoth.metadata.audible.models.AudibleRegions

object DevThothConfig : ThothConfig {
    override val ignoreFile: String by lazy { System.getenv("THOTH_IGNORE_FILE") ?: ".thothignore" }
    override val production: Boolean by lazy { System.getenv("THOTH_PRODUCTION")?.toBooleanStrictOrNull() ?: false }

    override val analyzerThreads: Int by lazy { System.getenv("THOTH_ANALYZER_THREADS")?.toIntOrNull() ?: 10 }

    override val port: Int by lazy { System.getenv("THOTH_PORT")?.toIntOrNull() ?: 8080 }

    override val domain: String by lazy { System.getenv("THOTH_DOMAIN") ?: "localhost" }

    override val TLS: Boolean by lazy { System.getenv("THOTH_TLS")?.toBooleanStrictOrNull() ?: false }

    override val audibleRegion: AudibleRegions
        get() = AudibleRegions.us

    override val database: DatabaseConnection by lazy { H2Database }
    override val configDirectory: String by lazy { "config" }
}
