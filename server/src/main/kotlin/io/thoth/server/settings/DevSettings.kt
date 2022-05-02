package io.thoth.server.settings

import java.security.KeyPair
import java.security.KeyPairGenerator


object DevSettings : Settings {
    override val ignoreFile: String by lazy {
        System.getenv("IGNORE_FILE") ?: ".audignore"
    }
    override val production = false

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
        System.getenv("AUDIBLE_AUTHOR_HOST") ?: "audible.com"
    }

    override val database: DatabaseConnection by lazy {
        H2Database
    }
    override val keyPair: KeyPair by lazy {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        kpg.generateKeyPair()
    }
}
