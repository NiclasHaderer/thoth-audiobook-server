package io.thoth.auth.models

open class JWK(
    open val kty: String,
    open val use: String,
    open val kid: String,
    open val n: String,
    open val e: String,
)
