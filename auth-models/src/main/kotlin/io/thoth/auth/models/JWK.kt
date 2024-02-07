package io.thoth.auth.models

open class JWK(val kty: String, val use: String, val kid: String, val n: String, val e: String)
