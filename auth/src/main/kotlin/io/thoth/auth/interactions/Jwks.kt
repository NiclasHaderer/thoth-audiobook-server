package io.thoth.auth.interactions

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import io.thoth.auth.models.JWK
import io.thoth.auth.models.JWKs
import io.thoth.auth.thothAuthConfig
import io.thoth.openapi.ktor.RouteHandler
import java.security.interfaces.RSAPublicKey

interface ThothJwksParams

fun RouteHandler.getJwks(params: ThothJwksParams): JWKs {
    val config = thothAuthConfig<Any, Any>()
    val keyPairs = config.keyPairs

    val jwks =
        keyPairs.entries
            .map { (keyId, keyPair) ->
                RSAKey
                    .Builder(keyPair.public as RSAPublicKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID(keyId)
                    .build()
            }.map { rsaKey ->
                JWK(
                    kty = rsaKey.keyType.toString(),
                    use = rsaKey.keyUse.toString(),
                    kid = rsaKey.keyID,
                    n = rsaKey.modulus.toString(),
                    e = rsaKey.publicExponent.toString(),
                )
            }

    return JWKs(keys = jwks)
}
