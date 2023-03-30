package io.thoth.server.api

import java.time.LocalDate

data class PartialAuthorApiModel(
    val name: String?,
    val provider: String?,
    val providerID: String?,
    val biography: String?,
    val image: String?,
    val website: String?,
    val bornIn: String?,
    val birthDate: LocalDate?,
    val deathDate: LocalDate?
)

data class AuthorApiModel(
    val name: String,
    val provider: String?,
    val providerID: String?,
    val biography: String?,
    val image: String?,
    val website: String?,
    val bornIn: String?,
    val birthDate: LocalDate?,
    val deathDate: LocalDate?
)
