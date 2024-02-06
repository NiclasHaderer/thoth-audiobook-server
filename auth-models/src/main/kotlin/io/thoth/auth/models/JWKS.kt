package io.thoth.auth.models

interface JWKs {
    val keys: List<JWK>
}

data class JWKsImpl(override val keys: List<JWK>) : JWKs
