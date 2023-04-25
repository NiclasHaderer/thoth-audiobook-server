package io.thoth.server.authentication.routes

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import io.thoth.generators.openapi.RouteHandler
import io.thoth.server.authentication.AuthConfigImpl
import java.security.interfaces.RSAPublicKey

data class JWK(val kty: String, val use: String, val kid: String, val n: String, val e: String)

data class JWKsResponse(val keys: List<JWK>)

internal fun jwksEndpoint(config: AuthConfigImpl): RouteHandler.() -> JWKsResponse {
    return {
        val keyPair = config.keyPair
        val jwk = RSAKey.Builder(keyPair.public as RSAPublicKey).keyUse(KeyUse.SIGNATURE).keyID(config.keyId).build()

        JWKsResponse(
            listOf(
                JWK(
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
