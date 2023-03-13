package io.thoth.auth

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.thoth.auth.routes.AuthRoutes
import io.thoth.auth.routes.authRoutes
import java.io.File
import java.io.FileReader
import java.nio.file.Path
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import kotlin.io.path.writer
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcaPEMWriter

interface AuthConfig {
    var keyPairPath: String?
    var keyId: String
    var issuer: String
    var domain: String?
    var jwksPath: String
    var protocol: URLProtocol
    var realm: String?
}

internal class AuthConfigImpl(
    val keyPair: KeyPair,
    val keyId: String,
    val issuer: String,
    val domain: String,
    val jwksPath: String,
    val protocol: URLProtocol,
    val realm: String? = null
)

private fun createKeyPair(jwtKeyFile: Path): KeyPair {
    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(2048, SecureRandom())
    val kp = kpg.generateKeyPair()

    JcaPEMWriter(jwtKeyFile.writer()).use { pemWriter -> pemWriter.writeObject(kp) }
    return kp
}

private fun getOrCreateKeyPair(path: Path): KeyPair {
    return try {
        FileReader(File("$path/jwt.pem")).use { reader ->
            val parsed: PEMKeyPair = PEMParser(reader).readObject() as PEMKeyPair
            JcaPEMKeyConverter().getKeyPair(parsed)
        }
    } catch (e: Exception) {
        createKeyPair(path)
    }
}

fun Application.configureAuthentication(configFactory: (AuthConfig.() -> Unit)? = null): AuthRoutes {
    val config =
        object : AuthConfig {
            override var keyPairPath: String? = null
            override var keyId: String = "thoth"
            override var issuer: String = "thoth"
            override var domain: String? = null
            override var jwksPath: String = "/api/.well-known/jwks.json"
            override var protocol: URLProtocol = URLProtocol.HTTP
            override var realm: String? = null
        }
    configFactory?.invoke(config)
    assert(config.domain != null) { "Domain must be set" }
    assert(config.keyPairPath != null) { "KeyPairPath must be set" }

    val configImpl =
        AuthConfigImpl(
            keyPair = getOrCreateKeyPair(Path.of(config.keyPairPath!!)),
            keyId = config.keyId,
            issuer = config.issuer,
            domain = config.domain!!,
            jwksPath = config.jwksPath,
            protocol = config.protocol,
            realm = config.realm
        )

    val url =
        URLBuilder()
            .apply {
                protocol = configImpl.protocol
                host = configImpl.domain
                encodedPath = configImpl.jwksPath
            }
            .build()
            .toURI()
            .toURL()

    val jwkProvider =
        JwkProviderBuilder(url).cached(5, 10, TimeUnit.MINUTES).rateLimited(10, 1, TimeUnit.MINUTES).build()

    install(Authentication) {
        jwt(Guards.Normal) {
            if (configImpl.realm != null) {
                realm = configImpl.realm
            }

            verifier(jwkProvider, configImpl.issuer) { acceptLeeway(3) }

            validate { jwtCredential ->
                val principal = jwtToPrincipal(jwtCredential) ?: return@validate null
                if (principal.type == JwtType.Access) principal else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token is not valid or has expired"))
            }
        }

        jwt(Guards.Edit) {
            if (configImpl.realm != null) {
                realm = configImpl.realm
            }
            verifier(jwkProvider, configImpl.issuer) { acceptLeeway(3) }
            validate { jwtCredential ->
                val principal = jwtToPrincipal(jwtCredential) ?: return@validate null
                if (principal.type == JwtType.Access && principal.edit) principal else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token is not valid or has expired"))
            }
        }

        jwt(Guards.Admin) {
            if (configImpl.realm != null) {
                realm = configImpl.realm
            }
            verifier(jwkProvider, configImpl.issuer) { acceptLeeway(3) }
            validate { jwtCredential ->
                val principal = jwtToPrincipal(jwtCredential) ?: return@validate null
                if (principal.type == JwtType.Access && principal.edit && principal.admin) principal else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token is not valid or has expired"))
            }
        }
    }
    return authRoutes(configImpl)
}
