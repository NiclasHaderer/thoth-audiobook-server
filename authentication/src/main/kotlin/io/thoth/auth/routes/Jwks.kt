package io.thoth.auth.routes

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.thoth.auth.AuthConfig
import java.security.interfaces.RSAPublicKey


fun Route.jwksEndpoint(config: AuthConfig) = get {
    val keyPair = config.keyPair
    val jwk = RSAKey.Builder(keyPair.public as RSAPublicKey)
        .keyUse(KeyUse.SIGNATURE)
        .keyID(config.keyId)
        .build()


    call.respond(mapOf("keys" to arrayOf(jwk.toJSONObject())))
}
