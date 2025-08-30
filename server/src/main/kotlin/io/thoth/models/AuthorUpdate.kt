package io.thoth.models

import java.time.LocalDate

data class AuthorUpdate(
    val name: String?,
    val provider: String?,
    val providerID: String?,
    val biography: String?,
    val image: String?,
    val website: String?,
    val bornIn: String?,
    val birthDate: LocalDate?,
    val deathDate: LocalDate?,
)
