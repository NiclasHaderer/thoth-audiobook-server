package io.thoth.auth.routes

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import io.thoth.auth.AuthConfigImpl
import io.thoth.openapi.RouteHandler
import java.security.interfaces.RSAPublicKey

typealias KeyId = String

data class Jwks(val kty: String, val use: String, val kid: String, val n: String, val e: String)

data class JwksResponse(val keys: Map<KeyId, Jwks>)

internal fun jwksEndpoint(config: AuthConfigImpl): RouteHandler.() -> JwksResponse {
    return {
        val keyPair = config.keyPair
        val jwk = RSAKey.Builder(keyPair.public as RSAPublicKey).keyUse(KeyUse.SIGNATURE).keyID(config.keyId).build()

        JwksResponse(
            mapOf(
                config.keyId to
                    Jwks(
                        kty = jwk.keyType.toString(),
                        use = jwk.keyUse.toString(),
                        kid = jwk.keyID,
                        n = jwk.modulus.toString(),
                        e = jwk.publicExponent.toString(),
                    ),
            ),
        )
    }
}
