package io.thoth.auth.models.impl

import io.thoth.auth.models.JWK
import io.thoth.auth.models.JWKs


internal data class JWKImpl(
    override val kty: String,
    override val use: String,
    override val kid: String,
    override val n: String,
    override val e: String
) : JWK

internal data class JWKsImpl(override val keys: List<JWK>) : JWKs
