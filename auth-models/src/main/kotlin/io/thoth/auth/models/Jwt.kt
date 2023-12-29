package io.thoth.auth.models

interface ThothJwtPair {
    val accessToken: String
    val refreshToken: String
}

data class ThothJwtPairImpl(
    override val accessToken: String,
    override val refreshToken: String,
) : ThothJwtPair

