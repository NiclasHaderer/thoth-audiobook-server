package io.thoth.server.config

import io.thoth.metadata.audible.client.AudibleRegions


object DevThothConfig : ThothConfig {
    override val ignoreFile: String by lazy {
        System.getenv("THOTH_IGNORE_FILE") ?: ".thothignore"
    }
    override val production = false

    override val audioFileLocations: List<String> by lazy {
        System.getenv("THOTH_AUDIO_FILE_LOCATION")?.split(",") ?: listOf("test-resources")
    }

    override val analyzerThreads: Int by lazy {
        System.getenv("THOTH_ANALYZER_THREADS")?.toIntOrNull() ?: 10
    }

    override val port: Int by lazy {
        System.getenv("THOTH_PORT")?.toIntOrNull() ?: 8080
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

