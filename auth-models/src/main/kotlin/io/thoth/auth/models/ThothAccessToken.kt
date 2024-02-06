package io.thoth.auth.models

interface ThothAccessToken {
    val accessToken: String
}

class ThothAccessTokenImpl(
    override val accessToken: String,
) : ThothAccessToken
