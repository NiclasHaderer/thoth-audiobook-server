package io.thoth.auth.models

data class JWK(val kty: String, val use: String, val kid: String, val n: String, val e: String)

data class JWKs(val keys: List<JWK>)
