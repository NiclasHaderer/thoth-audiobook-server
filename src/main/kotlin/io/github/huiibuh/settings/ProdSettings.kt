package io.github.huiibuh.settings

object ProdSettings : Settings {
    override val ignoreFile: String by lazy {
        System.getenv("IGNORE_FILE") ?: ".audignore"
    }
    override val production: Boolean by lazy {
        isProduction()
    }

    override val audioFileLocation: String by lazy {
        System.getenv("AUDIO_FILE_LOCATION")
    }

    override val webUiPort: Int by lazy {
        getPort()
    }

    override val audibleSearchHost: String by lazy {
        System.getenv("AUDIBLE_SEARCH_HOST")
    }
    override val audibleAuthorHost: String by lazy {
        System.getenv("AUDIBLE_AUTHOR_HOST")
    }

    override val database: DatabaseConnection by lazy {
        SqLite
    }
}
