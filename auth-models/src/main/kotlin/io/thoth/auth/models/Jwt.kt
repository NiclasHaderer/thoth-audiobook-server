package io.thoth.auth.models

open class ThothJwtPair(
    val accessToken: String,
    val refreshToken: String,
)
