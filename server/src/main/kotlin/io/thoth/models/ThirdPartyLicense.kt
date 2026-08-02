package io.thoth.models

import kotlinx.serialization.Serializable

@Serializable
data class ThirdPartyLicense(
    val name: String,
    val version: String,
    val license: String,
    val licenseUrl: String?,
    val repository: String?,
    val text: String?,
)
