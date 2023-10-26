package io.thoth.auth.models

interface JWK {
    val kty: String
    val use: String
    val kid: String
    val n: String
    val e: String
}

internal data class JWKImpl(
    override val kty: String,
    override val use: String,
    override val kid: String,
    override val n: String,
    override val e: String
) : JWK

interface JWKs {
    val keys: List<JWK>
}

internal data class JWKsImpl(override val keys: List<JWK>) : JWKs
