package io.thoth.auth.models

interface ThothJwtPair {
    val accessToken: String
    val refreshToken: String
}

internal data class ThothJwtPairImpl(
    override val accessToken: String,
    override val refreshToken: String,
) : ThothJwtPair

enum class ThothJwtTypes(val type: String) {
    Access("access"),
    Refresh("refresh")
}

interface ThothAccessToken {
    val accessToken: String
}

internal  class ThothAccessTokenImpl(
    override val accessToken: String,
) : ThothAccessToken
