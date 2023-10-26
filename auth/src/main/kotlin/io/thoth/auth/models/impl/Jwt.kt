package io.thoth.auth.models.impl

import io.thoth.auth.models.ThothAccessToken
import io.thoth.auth.models.ThothJwtPair


internal data class ThothJwtPairImpl(
    override val accessToken: String,
    override val refreshToken: String,
) : ThothJwtPair


internal class ThothAccessTokenImpl(
    override val accessToken: String,
) : ThothAccessToken
