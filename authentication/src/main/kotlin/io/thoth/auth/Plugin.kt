package io.thoth.auth

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.thoth.auth.routes.authRoutes
import java.io.File
import java.io.FileReader
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcaPEMWriter

class AuthConfig(
    var keyPair: KeyPair,
    var keyId: String,
    var issuer: String,
    var domain: String?,
    var basePath: String,
    var protocol: URLProtocol,
    var realm: String? = null
)

fun createKeyPair(jwtKeyFile: File): KeyPair {
  val kpg = KeyPairGenerator.getInstance("RSA")
  kpg.initialize(2048, SecureRandom())
  val kp = kpg.generateKeyPair()

  JcaPEMWriter(jwtKeyFile.writer()).use { pemWriter -> pemWriter.writeObject(kp) }
  return kp
}

fun getAuthConfig(configDir: String): AuthConfig {
  // Check if file exists
  val jwtKeyFile = File("$configDir/jwt.pem")

  val pair: KeyPair =
      try {
        FileReader(File("$configDir/jwt.pem")).use { reader ->
          val parsed: PEMKeyPair = PEMParser(reader).readObject() as PEMKeyPair
          JcaPEMKeyConverter().getKeyPair(parsed)
        }
      } catch (e: Exception) {
        createKeyPair(jwtKeyFile)
      }

  return AuthConfig(
      keyPair = pair,
      keyId = "thoth",
      issuer = "thoth",
      domain = null,
      protocol = URLProtocol.HTTP,
      basePath = "/api/auth")
}

fun Application.configureAuthentication(
    configDir: String,
    configFactory: (AuthConfig.() -> Unit)? = null
) {
  val config = getAuthConfig(configDir)
  configFactory?.invoke(config)
  assert(config.domain != null) { "Domain must be set" }

  val url =
      URLBuilder()
          .apply {
            protocol = config.protocol
            host = config.domain!!
            encodedPath = "${config.basePath}/.well-known/jwks.json".replace("//", "/")
          }
          .build()
          .toURI()
          .toURL()

  val jwkProvider =
      JwkProviderBuilder(url)
          .cached(5, 10, TimeUnit.MINUTES)
          .rateLimited(10, 1, TimeUnit.MINUTES)
          .build()

  install(Authentication) {
    jwt(GuardTypes.User.value) {
      if (config.realm != null) {
        realm = config.realm!!
      }

      verifier(jwkProvider, config.issuer) { acceptLeeway(3) }

      validate { jwtCredential ->
        val principal = jwtToPrincipal(jwtCredential) ?: return@validate null
        if (principal.type == JwtType.Access) principal else null
      }
      challenge { _, _ ->
        call.respond(
            HttpStatusCode.Unauthorized, mapOf("error" to "Token is not valid or has expired"))
      }
    }

    jwt(GuardTypes.EditUser.value) {
      if (config.realm != null) {
        realm = config.realm!!
      }
      verifier(jwkProvider, config.issuer) { acceptLeeway(3) }
      validate { jwtCredential ->
        val principal = jwtToPrincipal(jwtCredential) ?: return@validate null
        if (principal.type == JwtType.Access && principal.edit) principal else null
      }
      challenge { _, _ ->
        call.respond(
            HttpStatusCode.Unauthorized, mapOf("error" to "Token is not valid or has expired"))
      }
    }

    jwt(GuardTypes.AdminUser.value) {
      if (config.realm != null) {
        realm = config.realm!!
      }
      verifier(jwkProvider, config.issuer) { acceptLeeway(3) }
      validate { jwtCredential ->
        val principal = jwtToPrincipal(jwtCredential) ?: return@validate null
        if (principal.type == JwtType.Access && principal.edit && principal.admin) principal
        else null
      }
      challenge { _, _ ->
        call.respond(
            HttpStatusCode.Unauthorized, mapOf("error" to "Token is not valid or has expired"))
      }
    }
  }
  authRoutes(config)
}
