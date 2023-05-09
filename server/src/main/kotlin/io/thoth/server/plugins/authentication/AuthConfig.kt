package io.thoth.server.plugins.authentication

import io.ktor.http.*
import java.nio.file.Path
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import kotlin.io.path.reader
import kotlin.io.path.writer
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcaPEMWriter

private fun createKeyPair(jwtKeyFile: Path): KeyPair {
    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(2048, SecureRandom())
    val kp = kpg.generateKeyPair()

    JcaPEMWriter(jwtKeyFile.writer()).use { pemWriter ->
        pemWriter.writeObject(kp.private)
        pemWriter.writeObject(kp.public)
    }
    return kp
}

private fun getOrCreateKeyPair(path: Path): KeyPair {
    return try {
        path.reader().use { reader ->
            val parsed: PEMKeyPair = PEMParser(reader).readObject() as PEMKeyPair
            JcaPEMKeyConverter().getKeyPair(parsed)
        }
    } catch (e: Exception) {
        createKeyPair(path)
    }
}

data class AuthConfig(
    val keyPair: KeyPair,
    val keyId: String,
    val issuer: String,
    val domain: String,
    val jwksPath: String,
    val protocol: URLProtocol,
    val realm: String? = null
) {
    class Builder {
        var keyPairPath: String? = null
        var keyId: String = "thoth"
        var issuer: String = "thoth"
        var domain: String? = null
        var jwksPath: String = "/api/auth/.well-known/jwks.json"
        var protocol: URLProtocol = URLProtocol.HTTP
        var realm: String? = null

        fun configure(cb: Builder.() -> Unit): Builder {
            cb()
            return this
        }

        fun build(): AuthConfig {
            assert(domain != null) { "Domain must be set" }
            assert(keyPairPath != null) { "KeyPairPath must be set" }
            val keyPair = getOrCreateKeyPair(Path.of(keyPairPath!!))
            return AuthConfig(
                keyPair = keyPair,
                keyId = keyId,
                issuer = issuer,
                domain = domain ?: "localhost",
                jwksPath = jwksPath,
                protocol = protocol,
                realm = realm,
            )
        }
    }
}
