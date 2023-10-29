package io.thoth.auth.models

interface JWK {
    val kty: String
    val use: String
    val kid: String
    val n: String
    val e: String
}

interface JWKs {
    val keys: List<JWK>
}
