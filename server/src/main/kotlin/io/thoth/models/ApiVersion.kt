package io.thoth.models

import kotlinx.serialization.Serializable

@Serializable
data class ApiVersion(
    val version: String,
)
