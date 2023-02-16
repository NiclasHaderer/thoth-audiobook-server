package io.thoth.config.public

import io.thoth.metadata.audible.models.AudibleRegions


object DevPublicConfig : PublicConfig {
    override val ignoreFile: String by lazy {
        System.getenv("THOTH_IGNORE_FILE") ?: ".thothignore"
    }
    override val production: Boolean by lazy {
        System.getenv("THOTH_PRODUCTION")?.toBooleanStrictOrNull() ?: false
    }

    override val audioFileLocations: List<String> by lazy {
        System.getenv("THOTH_AUDIO_FILE_LOCATION")?.split(",") ?: listOf("test-resources")
    }

    override val analyzerThreads: Int by lazy {
        System.getenv("THOTH_ANALYZER_THREADS")?.toIntOrNull() ?: 10
    }

    override val port: Int by lazy {
        System.getenv("THOTH_PORT")?.toIntOrNull() ?: 8080
    }

    override val domain: String by lazy {
        System.getenv("THOTH_DOMAIN") ?: "localhost"
    }

    override val TLS: Boolean by lazy {
        System.getenv("THOTH_TLS")?.toBooleanStrictOrNull() ?: false
    }

    override val audibleRegion: AudibleRegions
        get() = AudibleRegions.us

    override val database: DatabaseConnection by lazy {
        H2Database
    }
    override val configDirectory: String by lazy {
        "config"
    }
}

