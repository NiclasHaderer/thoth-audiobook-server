package io.github.huiibuh.settings

object DevSettings : Settings {
    override val ignoreFile: String by lazy {
        System.getenv("IGNORE_FILE") ?: ".audignore"
    }
    override val production: Boolean by lazy {
        isProduction()
    }

    override val audioFileLocation: String by lazy {
        System.getenv("AUDIO_FILE_LOCATION") ?: "test-resources"
    }


    override val analyzerThreads: Int by lazy {
        System.getenv("ANALYZER_THREADS")?.toIntOrNull() ?: 10
    }

    override val webUiPort: Int by lazy {
        getPort()
    }

    override val audibleSearchHost: String by lazy {
        System.getenv("AUDIBLE_SEARCH_HOST") ?: "audible.de"
    }
    override val audibleAuthorHost: String by lazy {
        System.getenv("AUDIBLE_AUTHOR_HOST") ?: "audible.de"
    }

    override val database: DatabaseConnection by lazy {
        SqLite
    }
}
