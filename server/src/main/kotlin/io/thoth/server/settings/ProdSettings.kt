package io.thoth.server.settings

import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.X509EncodedKeySpec


object ProdSettings : Settings {
    override val ignoreFile: String by lazy {
        System.getenv("IGNORE_FILE") ?: ".audignore"
    }
    override val production = true

    override val audioFileLocation: String by lazy {
        System.getenv("AUDIO_FILE_LOCATION")
    }

    override val analyzerThreads: Int by lazy {
        System.getenv("ANALYZER_THREADS").toInt()
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
        ProdDatabaseConnection
    }

    override val keyPair: KeyPair by lazy {
        val keyFactory: KeyFactory = KeyFactory.getInstance("RSA")

        var publicKeyContent = System.getenv("PUBLIC_KEY")?.toByteArray()

        if (publicKeyContent == null) {
            val publicKeyLocation = System.getenv("PUBLIC_KEY_LOCATION")
            publicKeyContent = Files.readAllBytes(Paths.get(publicKeyLocation))
        }

        val publicSpec = X509EncodedKeySpec(publicKeyContent)
        val publicKey = keyFactory.generatePublic(publicSpec)

        var privateKeyContent = System.getenv("PRIVATE_KEY")?.toByteArray()

        if (privateKeyContent == null) {
            val privateKeyLocation = System.getenv("PRIVATE_KEY_LOCATION")
            privateKeyContent = Files.readAllBytes(Paths.get(privateKeyLocation))
        }

        val privateSpec = X509EncodedKeySpec(privateKeyContent)
        val privateKey = keyFactory.generatePrivate(privateSpec)

        KeyPair(publicKey, privateKey)
    }

}
