package io.thoth.auth.routes

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import io.thoth.auth.AuthConfigImpl
import io.thoth.openapi.routing.RouteHandler
import java.security.interfaces.RSAPublicKey

class JwksResponse(val keys: Map<String, Any>)

internal fun jwksEndpoint(config: AuthConfigImpl): RouteHandler.() -> JwksResponse {
    return {
        val keyPair = config.keyPair
        val jwk = RSAKey.Builder(keyPair.public as RSAPublicKey).keyUse(KeyUse.SIGNATURE).keyID(config.keyId).build()

        JwksResponse(jwk.toJSONObject())
    }
}
